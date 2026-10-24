import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Provider } from "react-redux";
import App from "./App";
import { checkout } from "./cartSlice";
import store from "./store";

test("shows a message when products cannot be loaded", async () => {
  global.fetch = jest.fn(() => Promise.reject(new TypeError("Failed to fetch")));
  render(
    <Provider store={store}>
      <App />
    </Provider>
  );
  expect(await screen.findByText("Cannot reach the server")).toBeInTheDocument();
});

test("renders product cards", async () => {
  global.fetch = jest.fn(() =>
    Promise.resolve({
      ok: true,
      json: () =>
        Promise.resolve([
          { id: 1, title: "Backpack", price: 109.95, image: "https://example.com/bag.jpg" },
        ]),
    })
  );
  render(
    <Provider store={store}>
      <App />
    </Provider>
  );
  expect(await screen.findByText("Products")).toBeInTheDocument();
  expect(await screen.findByText("Backpack")).toBeInTheDocument();
  expect(screen.getByText("$109.95")).toBeInTheDocument();
  expect(screen.getByText("Add to Cart")).toBeInTheDocument();
});

function token() {
  const payload = btoa(JSON.stringify({ sub: "a@b.com", exp: Math.floor(Date.now() / 1000) + 3600 }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
  return `h.${payload}.s`;
}

test("asks for login before adding to cart", async () => {
  localStorage.removeItem("token");
  global.fetch = jest.fn(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve([{ id: 1, title: "Backpack", price: 109.95, image: "https://example.com/bag.jpg" }]),
    })
  );
  render(
    <Provider store={store}>
      <App />
    </Provider>
  );
  await userEvent.click(await screen.findByRole("button", { name: "Add to Cart" }));
  expect(await screen.findByText("Please log in")).toBeInTheDocument();
  expect(screen.queryByLabelText("Cart").textContent).not.toMatch(/1/);
});

test("cart add, quantity, remove, and checkout", async () => {
  store.dispatch(checkout());
  localStorage.setItem("token", token());
  global.fetch = jest.fn((url) => {
    if (String(url).includes("/api/checkout")) {
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ orderId: 1, status: "CONFIRMED" }),
      });
    }
    return Promise.resolve({
      ok: true,
      json: () =>
        Promise.resolve([
          { id: 1, title: "Backpack", price: 109.95, image: "https://example.com/bag.jpg" },
        ]),
    });
  });
  render(
    <Provider store={store}>
      <App />
    </Provider>
  );
  await userEvent.click(await screen.findByRole("button", { name: "Add to Cart" }));
  await userEvent.click(screen.getByRole("button", { name: "Cart" }));
  const quantity = () => document.querySelector(".qty span").textContent;
  expect(quantity()).toBe("1");
  await userEvent.click(screen.getByRole("button", { name: "Increase Backpack" }));
  expect(quantity()).toBe("2");
  await userEvent.click(screen.getByRole("button", { name: "Decrease Backpack" }));
  expect(quantity()).toBe("1");
  await userEvent.click(screen.getByRole("button", { name: "Remove" }));
  expect(screen.queryByRole("button", { name: "Remove" })).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Add to Cart" }));
  await userEvent.click(screen.getByRole("button", { name: "Checkout" }));
  expect(await screen.findByText("Order placed")).toBeInTheDocument();
  expect(document.querySelector(".drawer").className).not.toMatch(/open/);
  expect(screen.queryByRole("button", { name: "Remove" })).not.toBeInTheDocument();
});
