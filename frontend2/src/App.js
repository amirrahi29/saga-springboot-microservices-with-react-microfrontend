import { useEffect, useState } from "react";
import { Provider, useDispatch, useSelector } from "react-redux";
import "./App.css";
import { addToCart, checkout, removeFromCart, updateQuantity } from "./cartSlice";
import store from "./store";

const API = "http://localhost:8082";

function sessionEmail() {
  const token = localStorage.getItem("token");
  if (!token) {
    return "";
  }
  try {
    const part = token.split(".")[1];
    const payload = JSON.parse(atob(part.replace(/-/g, "+").replace(/_/g, "/")));
    if (!payload.sub || (payload.exp && payload.exp * 1000 < Date.now())) {
      return "";
    }
    return payload.sub;
  } catch {
    return "";
  }
}

function App() {
  const [products, setProducts] = useState([]);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [open, setOpen] = useState(false);
  const items = useSelector((state) => state.cart.items);
  const dispatch = useDispatch();
  const count = items.reduce((total, item) => total + item.quantity, 0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const response = await fetch(`${API}/api/products`);
        if (cancelled) {
          return;
        }
        if (!response.ok) {
          setError(response.status >= 500 ? "Server error" : "Could not load products");
          return;
        }
        setProducts(await response.json());
      } catch {
        if (!cancelled) {
          setError("Cannot reach the server");
        }
      }
      if (!cancelled) {
        setReady(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  function add(product) {
    if (!sessionEmail()) {
      setNotice("Please log in");
      return;
    }
    setNotice("");
    dispatch(addToCart(product));
  }

  async function placeOrder() {
    const email = sessionEmail();
    if (!email) {
      setNotice("Please log in");
      return;
    }
    try {
      const response = await fetch(`${API}/api/checkout`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email,
          items: items.map((item) => ({
            productId: item.id,
            title: item.title,
            price: item.price,
            quantity: item.quantity,
          })),
        }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        if (response.status === 402) {
          dispatch(checkout());
        }
        setNotice(data.message || "Payment failed");
        return;
      }
      dispatch(checkout());
      setOpen(false);
      setNotice("Order placed");
    } catch {
      setNotice("Cannot reach the server");
    }
  }

  if (!ready) {
    return <div className="page" />;
  }

  return (
    <div className="page">
      <header className="header">
        <p className="eyebrow">Products</p>
        <button type="button" className="cart-button" aria-label="Cart" onClick={() => setOpen(true)}>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M6 6h15l-1.5 9h-12z" />
            <path d="M6 6 5 3H2" />
            <circle cx="9" cy="20" r="1.4" />
            <circle cx="18" cy="20" r="1.4" />
          </svg>
          {count > 0 && <span className="badge">{count}</span>}
        </button>
      </header>
      <main className="shop">
        {error && <p className="error">{error}</p>}
        {notice && <p className="error">{notice}</p>}
        <div className="grid">
          {products.map((product) => (
            <article className="card" key={product.id}>
              <img src={product.image} alt={product.title} />
              <h2>{product.title}</h2>
              <p className="price">${product.price}</p>
              <button type="button" className="add" onClick={() => add(product)}>
                Add to Cart
              </button>
            </article>
          ))}
        </div>
      </main>
      <button
        type="button"
        className={open ? "backdrop open" : "backdrop"}
        aria-label="Close cart"
        onClick={() => setOpen(false)}
      />
      <aside className={open ? "drawer open" : "drawer"}>
        <div className="drawer-head">
          <p className="eyebrow">Cart</p>
          <button type="button" className="close" onClick={() => setOpen(false)}>
            Close
          </button>
        </div>
        <div className="drawer-items">
          {items.map((item) => (
            <div className="line" key={item.id}>
              <img src={item.image} alt="" />
              <div>
                <p className="line-title">{item.title}</p>
                <p className="price">${item.price}</p>
                <div className="qty">
                  <button
                    type="button"
                    aria-label={`Decrease ${item.title}`}
                    onClick={() => dispatch(updateQuantity({ id: item.id, quantity: item.quantity - 1 }))}
                  >
                    −
                  </button>
                  <span>{item.quantity}</span>
                  <button
                    type="button"
                    aria-label={`Increase ${item.title}`}
                    onClick={() => dispatch(updateQuantity({ id: item.id, quantity: item.quantity + 1 }))}
                  >
                    +
                  </button>
                </div>
              </div>
              <button type="button" className="remove" onClick={() => dispatch(removeFromCart(item.id))}>
                Remove
              </button>
            </div>
          ))}
        </div>
        <button
          type="button"
          className="checkout"
          onClick={placeOrder}
        >
          Checkout
        </button>
      </aside>
    </div>
  );
}

export default function ProvidedApp() {
  return (
    <Provider store={store}>
      <App />
    </Provider>
  );
}
