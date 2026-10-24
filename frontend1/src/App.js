import { useEffect, useState } from "react";
import "./App.css";

const API = "http://localhost:8081";

let refreshRequest = null;

function refreshAccess() {
  if (!refreshRequest) {
    refreshRequest = (async () => {
      const refreshToken = localStorage.getItem("refreshToken");
      if (!refreshToken) {
        return false;
      }
      try {
        const response = await fetch(`${API}/api/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
        if (!response.ok) {
          return false;
        }
        const data = await response.json();
        localStorage.setItem("token", data.token);
        localStorage.setItem("refreshToken", data.refreshToken);
        return true;
      } catch {
        return null;
      }
    })().finally(() => {
      refreshRequest = null;
    });
  }
  return refreshRequest;
}

async function authFetch(url) {
  const send = () =>
    fetch(url, {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    });
  try {
    let response = await send();
    if (response.status !== 401) {
      return response;
    }
    const refreshed = await refreshAccess();
    if (refreshed == null) {
      return null;
    }
    if (!refreshed) {
      localStorage.removeItem("token");
      localStorage.removeItem("refreshToken");
      return response;
    }
    return await send();
  } catch {
    return null;
  }
}

function App() {
  const [route, setRoute] = useState(window.location.pathname);
  const [ready, setReady] = useState(false);
  const [tab, setTab] = useState("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [image, setImage] = useState(null);
  const [userName, setUserName] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    const onPop = () => setRoute(window.location.pathname);
    window.addEventListener("popstate", onPop);
    return () => window.removeEventListener("popstate", onPop);
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!localStorage.getItem("token") && !localStorage.getItem("refreshToken")) {
        if (!cancelled) {
          setReady(true);
        }
        return;
      }
      try {
        const home = await authFetch(`${API}/api/home`);
        if (cancelled) {
          return;
        }
        if (!home) {
          setError("Cannot reach the server");
        } else if (home.ok) {
          const data = await home.json();
          setUserName(data.name);
          setImageUrl(data.imageUrl || "");
        } else if (home.status >= 500) {
          setError("Server error");
        }
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

  useEffect(() => {
    if (!ready) {
      return;
    }
    const next = userName ? "/dashboard" : "/login";
    if (window.location.pathname !== next) {
      window.history.replaceState({}, "", next);
      setRoute(next);
    }
  }, [ready, userName, route]);

  async function register(event) {
    event.preventDefault();
    setError("");
    const form = new FormData();
    form.append("name", name);
    form.append("email", email);
    form.append("password", password);
    if (image) {
      form.append("image", image);
    }
    try {
      const response = await fetch(`${API}/api/auth/register`, {
        method: "POST",
        body: form,
      });
      if (!response.ok) {
        setError(response.status >= 500 ? "Server error" : "Register failed");
        return;
      }
      setTab("login");
      setPassword("");
      setImage(null);
    } catch {
      setError("Cannot reach the server");
    }
  }

  async function login(event) {
    event.preventDefault();
    setError("");
    console.info("Login button clicked", { email });
    try {
      const response = await fetch(`${API}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      console.info("Login response", response.status);
      if (!response.ok) {
        setError(response.status >= 500 ? "Server error" : "Login failed");
        return;
      }
      const data = await response.json();
      localStorage.setItem("token", data.token);
      localStorage.setItem("refreshToken", data.refreshToken);
      const home = await authFetch(`${API}/api/home`);
      if (!home) {
        setError("Cannot reach the server");
        return;
      }
      console.info("Home response", home.status);
      if (!home.ok) {
        setError(home.status >= 500 ? "Server error" : "Login failed");
        return;
      }
      const homeData = await home.json();
      console.info("Logged in as", homeData.name);
      setUserName(homeData.name);
      setImageUrl(data.imageUrl || homeData.imageUrl || "");
    } catch {
      setError("Cannot reach the server");
    }
  }

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    setUserName("");
    setImageUrl("");
  }

  if (!ready) {
    return <div className="page" />;
  }

  return (
    <div className="page">
      <div className="card">
        {userName ? (
          <div className="welcome">
            <p className="eyebrow">Dashboard</p>
            {imageUrl ? (
              <img className="avatar-photo" src={`${API}${imageUrl}`} alt={userName} />
            ) : (
              <div className="avatar">{userName.charAt(0).toUpperCase()}</div>
            )}
            <p className="logged-in">{userName}</p>
            <button type="button" className="logout" onClick={logout}>
              Logout
            </button>
          </div>
        ) : (
          <>
            <div className="tabs">
              <button
                type="button"
                className={tab === "login" ? "active" : ""}
                onClick={() => setTab("login")}
              >
                Login
              </button>
              <button
                type="button"
                className={tab === "register" ? "active" : ""}
                onClick={() => setTab("register")}
              >
                Register
              </button>
            </div>
            <form onSubmit={tab === "login" ? login : register}>
              {tab === "register" && (
                <label>
                  Name
                  <input
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    required
                  />
                </label>
              )}
              <label>
                Email
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  required
                />
              </label>
              <label>
                Password
                <input
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  required
                />
              </label>
              {tab === "register" && (
                <label>
                  Profile photo
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(event) => setImage(event.target.files[0] || null)}
                  />
                </label>
              )}
              {error && <p className="error">{error}</p>}
              <button className="submit" type="submit">
                {tab === "login" ? "Login" : "Register"}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}

export default App;