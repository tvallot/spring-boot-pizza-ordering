/**
 * Thin wrapper around fetch() for the Pizza Ordering REST API.
 * Handles the JWT, JSON bodies and RFC 7807 error responses.
 */
const api = (() => {
    const TOKEN_KEY = "pizza.token";
    const LOGIN_KEY = "pizza.login";

    /** Session storage may be unavailable (private mode…): fall back to memory. */
    const memory = {};
    const storage = {
        get: (key) => { try { return sessionStorage.getItem(key); } catch { return memory[key] ?? null; } },
        set: (key, value) => { try { sessionStorage.setItem(key, value); } catch { memory[key] = value; } },
        remove: (key) => { try { sessionStorage.removeItem(key); } catch { delete memory[key]; } }
    };

    /** Error carrying the HTTP status and the "detail" of the problem returned by the API. */
    class ApiError extends Error {
        constructor(status, message) {
            super(message);
            this.status = status;
        }
    }

    async function request(method, path, body) {
        const headers = { "Accept": "application/json" };
        const token = storage.get(TOKEN_KEY);
        if (token) {
            headers["Authorization"] = "Bearer " + token;
        }
        if (body !== undefined) {
            headers["Content-Type"] = "application/json";
        }

        const response = await fetch(window.API_BASE_URL + path, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body)
        });

        if (response.status === 401 && token) {
            session.clear(); // expired or revoked token
        }
        if (!response.ok) {
            let detail = response.statusText;
            try {
                detail = (await response.json()).detail || detail;
            } catch { /* no JSON body */ }
            throw new ApiError(response.status, detail);
        }
        return response.status === 204 ? null : response.json();
    }

    const session = {
        login: () => storage.get(LOGIN_KEY),
        isAuthenticated: () => storage.get(TOKEN_KEY) !== null,
        save(login, token) {
            storage.set(LOGIN_KEY, login);
            storage.set(TOKEN_KEY, token);
        },
        clear() {
            storage.remove(LOGIN_KEY);
            storage.remove(TOKEN_KEY);
        }
    };

    const encode = encodeURIComponent;

    return {
        ApiError,
        session,
        getMenu: () => request("GET", "/menu"),
        getCustomers: () => request("GET", "/customers"),
        register: (login, name, password) => request("POST", "/customers", { login, name, password }),
        async login(login, password) {
            const { token } = await request("POST", "/auth/login", { login, password });
            session.save(login, token);
        },
        async logout() {
            try {
                await request("POST", "/auth/logout");
            } finally {
                session.clear();
            }
        },
        getCustomer: (login) => request("GET", `/customers/${encode(login)}`),
        renameCustomer: (login, name) => request("PUT", `/customers/${encode(login)}`, { name }),
        deleteCustomer: (login) => request("DELETE", `/customers/${encode(login)}`),
        getOrders: () => request("GET", "/orders"),
        getOrder: (id) => request("GET", `/orders/${id}`),
        createOrder: (pizzas) => request("POST", "/orders", { pizzas }),
        addPizza: (id, pizzaName) => request("POST", `/orders/${id}/pizzas`, { pizzaName }),
        removePizza: (id, pizzaName) => request("DELETE", `/orders/${id}/pizzas/${encode(pizzaName)}`),
        payOrder: (id, amount) => request("POST", `/orders/${id}/payment`, { amount }),
        cancelOrder: (id) => request("DELETE", `/orders/${id}`)
    };
})();
