/**
 * Single-page application: hash-based routing, Mustache rendering and event delegation.
 */
(() => {
    const view = document.getElementById("view");
    const toastElement = document.getElementById("toast");
    const price = new Intl.NumberFormat("en-IE", { style: "currency", currency: "EUR" });
    const STATUS_LABELS = { AWAITING_PAYMENT: "Awaiting payment", PREPARING: "In the oven", READY: "Ready" };
    const POLL_INTERVAL_MS = 2000;

    let menuCache = null;
    let basket = [];
    let currentOrder = null;
    let pollTimer = null;

    // ------------------------------------------------------------------ helpers

    function render(templateId, data = {}) {
        const template = document.getElementById(templateId).innerHTML;
        view.innerHTML = Mustache.render(template, data);
    }

    let toastTimer;
    function toast(message, type = "success") {
        toastElement.textContent = message;
        toastElement.className = `alert alert-${type}`;
        toastElement.hidden = false;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { toastElement.hidden = true; }, 4000);
    }

    function refreshNavigation() {
        document.body.classList.toggle("authenticated", api.session.isAuthenticated());
        document.getElementById("nav-login").textContent = api.session.login() || "";
    }

    async function getMenu() {
        if (!menuCache) {
            const { pizzas } = await api.getMenu();
            menuCache = pizzas.map(pizza => ({
                ...pizza,
                priceLabel: price.format(pizza.price),
                ingredientList: pizza.ingredients.map(i => i.name).join(", ")
            }));
        }
        return menuCache;
    }

    function decorateOrder(order) {
        const editable = order.status === "AWAITING_PAYMENT";
        return {
            ...order,
            editable,
            preparing: order.status === "PREPARING",
            priceLabel: price.format(order.price),
            statusLabel: STATUS_LABELS[order.status],
            pizzas: order.pizzas.map(pizza => ({ ...pizza, editable, priceLabel: price.format(pizza.price) }))
        };
    }

    function requireAuthentication() {
        if (!api.session.isAuthenticated()) {
            location.hash = "#login";
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ views

    const routes = {
        async home() {
            const [pizzas, customers] = await Promise.all([getMenu(), api.getCustomers()]);
            render("tpl-home", { pizzas, customerCount: customers.count });
        },

        register() {
            render("tpl-register");
        },

        login() {
            render("tpl-login", { login: api.session.login() });
        },

        async account() {
            if (!requireAuthentication()) return;
            render("tpl-account", await api.getCustomer(api.session.login()));
        },

        async orders() {
            if (!requireAuthentication()) return;
            const { orders } = await api.getOrders();
            render("tpl-orders", { orders: orders.reverse().map(decorateOrder) });
        },

        async "new-order"() {
            if (!requireAuthentication()) return;
            render("tpl-new-order", { pizzas: await getMenu() });
            renderBasket();
        },

        async order(id) {
            if (!requireAuthentication()) return;
            const [menu, order] = await Promise.all([getMenu(), api.getOrder(id)]);
            currentOrder = order;
            render("tpl-order", { ...decorateOrder(order), menu });
            if (order.status === "PREPARING") {
                pollTimer = setTimeout(() => routes.order(id).catch(showError), POLL_INTERVAL_MS);
            }
        }
    };

    function renderBasket() {
        const items = basket.map((pizza, index) => ({ ...pizza, index }));
        const total = basket.reduce((sum, pizza) => sum + pizza.price, 0);
        const template = document.getElementById("tpl-basket").innerHTML;
        document.getElementById("basket").innerHTML =
            Mustache.render(template, { items, totalLabel: price.format(total) });
    }

    function showError(error) {
        if (error instanceof api.ApiError && error.status === 401) {
            refreshNavigation();
            toast("Please log in again.", "warning");
            location.hash = "#login";
            return;
        }
        render("tpl-error", {
            title: error.status ? `Error ${error.status}` : "Something went wrong",
            message: error.message
        });
    }

    async function navigate() {
        clearTimeout(pollTimer);
        refreshNavigation();
        const [route, param] = location.hash.replace(/^#/, "").split("/");
        const handler = routes[route || "home"];
        try {
            if (handler) {
                await handler(param);
            } else {
                render("tpl-error", { title: "Page not found", message: "This page does not exist." });
            }
        } catch (error) {
            showError(error);
        }
    }

    // ------------------------------------------------------------------ actions

    const forms = {
        async register(data) {
            await api.register(data.login, data.name, data.password);
            await api.login(data.login, data.password);
            toast(`Welcome, ${data.name}!`);
            location.hash = "#new-order";
        },

        async login(data) {
            await api.login(data.login, data.password);
            toast("Logged in.");
            location.hash = "#home";
        },

        async rename(data) {
            await api.renameCustomer(api.session.login(), data.name);
            toast("Name updated.");
            await navigate();
        },

        async "order-add"(data) {
            await api.addPizza(currentOrder.id, data.pizzaName);
            await navigate();
        }
    };

    const actions = {
        async logout() {
            await api.logout();
            toast("Logged out.");
            location.hash = "#home";
            refreshNavigation();
        },

        async "delete-account"() {
            if (!confirm("Delete your account and all your orders?")) return;
            await api.deleteCustomer(api.session.login());
            api.session.clear();
            toast("Account deleted.");
            location.hash = "#home";
        },

        async "basket-add"(target) {
            const pizza = (await getMenu()).find(p => p.name === target.dataset.pizza);
            basket.push(pizza);
            renderBasket();
        },

        "basket-remove"(target) {
            basket.splice(Number(target.dataset.index), 1);
            renderBasket();
        },

        async "basket-submit"() {
            const order = await api.createOrder(basket.map(pizza => pizza.name));
            basket = [];
            toast(`Order ${order.id} created.`);
            location.hash = `#order/${order.id}`;
        },

        async "order-remove"(target) {
            await api.removePizza(currentOrder.id, target.dataset.pizza);
            await navigate();
        },

        async "order-pay"() {
            if (!confirm(`Pay ${price.format(currentOrder.price)}?`)) return;
            await api.payOrder(currentOrder.id, currentOrder.price);
            toast("Payment accepted, your pizzas are in the oven!");
            await navigate();
        },

        async "order-cancel"() {
            if (!confirm("Cancel this order?")) return;
            await api.cancelOrder(currentOrder.id);
            toast("Order cancelled.");
            location.hash = "#orders";
        }
    };

    document.addEventListener("submit", async (event) => {
        const form = event.target.closest("[data-form]");
        if (!form) return;
        event.preventDefault();
        try {
            await forms[form.dataset.form](Object.fromEntries(new FormData(form)));
        } catch (error) {
            if (error.status === 401) refreshNavigation();
            toast(error.message, "danger");
        }
    });

    document.addEventListener("click", async (event) => {
        const target = event.target.closest("[data-action]");
        if (!target) return;
        event.preventDefault();
        try {
            await actions[target.dataset.action](target);
        } catch (error) {
            toast(error.message, "danger");
        }
    });

    window.addEventListener("hashchange", navigate);
    navigate();
})();
