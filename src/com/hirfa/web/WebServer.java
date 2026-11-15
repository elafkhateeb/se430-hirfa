package com.hirfa.web;

import com.hirfa.model.*;
import com.hirfa.repo.Repo;
import com.hirfa.service.*;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServer {
    private final Repo repo;
    private final CustomerService customerService;
    private final ArtisanService artisanService;
    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final WalletService walletService;
    private final ReportService reportService;
    private final EtaCalculator eta = new EtaCalculator();

    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private static final String COOKIE = "hirfa_sid";

    private static class Session {
        Customer user;
        Cart cart = new Cart();
    }

    public WebServer(Repo repo) {
        this.repo = repo;
        this.customerService = new CustomerService(repo);
        this.artisanService = new ArtisanService(repo);
        this.productService = new ProductService(repo);
        this.cartService = new CartService(repo);
        this.orderService = new OrderService(repo);
        this.walletService = new WalletService(repo);
        this.reportService = new ReportService(repo);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::route);
        server.start();
        System.out.println("Hirfa platform started: http://localhost:" + port);
    }

    private void route(com.sun.net.httpserver.HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        boolean post = ex.getRequestMethod().equals("POST");
        String body = post ? new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8) : "";
        String query = body.isEmpty() ? ex.getRequestURI().getRawQuery() : body;
        var params = parse(query == null ? "" : query);
        String sid = sid(ex);
        Session s = sessions.computeIfAbsent(sid, k -> new Session());
        String cookieOld = existingCookie(ex);

        if (path.equals("/logout")) {
            sessions.remove(sid);
            redirect(ex, COOKIE + "=; Path=/; Max-Age=0", "/");
            return;
        }

        String page;
        switch (path) {
            case "/", "/products" -> page = products(params.get("craft"), params.get("max"), s);
            case "/artisan" -> page = post ? doRateArtisan(params.get("id"), params.get("stars"), s)
                    : artisanPage(params.get("id"), s);
            case "/register" -> page = post ? doRegister(params, s) : registerPage(null, s);
            case "/login" -> page = post ? doLogin(params, s) : loginPage(null, s);
            case "/cart" -> page = post ? cartServiceSubmit(params, s) : cartPage(null, s);
            case "/order" -> page = post ? doOrder(params, s) : checkoutPage(null, s);
            case "/wallet" -> page = post ? doWallet(params, s) : walletPage(null, s);
            case "/orders" -> page = ordersPage(s);
            case "/admin" -> page = adminPage(params.get("id"), params.get("from"), params.get("to"), s);
            default -> page = notFound(s);
        }
        byte[] bytes = page.getBytes(StandardCharsets.UTF_8);
        if (!sid.equals(cookieOld)) ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + sid + "; Path=/; HttpOnly");
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void redirect(com.sun.net.httpserver.HttpExchange ex, String cookie, String to) throws IOException {
        if (cookie != null) ex.getResponseHeaders().add("Set-Cookie", cookie);
        ex.getResponseHeaders().set("Location", to);
        ex.sendResponseHeaders(302, -1);
    }

    private String sid(com.sun.net.httpserver.HttpExchange ex) {
        return existingCookie(ex).isEmpty() ? UUID.randomUUID().toString() : existingCookie(ex);
    }

    private String existingCookie(com.sun.net.httpserver.HttpExchange ex) {
        String c = ex.getRequestHeaders().getFirst("Cookie");
        if (c != null) for (String part : c.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(COOKIE)) return kv[1];
        }
        return "";
    }

    /* ---------- bilingual display layer (UI is 100% English) ---------- */

    private static final List<String> CRAFTS = List.of("تطريز", "صابون", "خشبيات", "جلوديات", "سيراميك", "مجوهرات");
    private static final Map<String, String> CRAFT_EN = Map.of(
            "تطريز", "Embroidery", "صابون", "Soap", "خشبيات", "Woodwork",
            "جلوديات", "Leather", "سيراميك", "Ceramics", "مجوهرات", "Jewelry");
    private static final Map<String, String> NAME_EN = Map.ofEntries(
            Map.entry("أمل الخطيب", "Amal Al-Khatib"), Map.entry("ريم الشوبكي", "Reem Al-Shobaki"),
            Map.entry("سمية المجالي", "Samia Al-Majali"), Map.entry("ليان الزعبي", "Layan Al-Zubi"),
            Map.entry("محمد العمري", "Mohammad Al-Omari"));
    private static final Map<String, String> PRODUCT_EN = Map.ofEntries(
            Map.entry("مفرش كرسي مطرز", "Embroidered Chair Cover"),
            Map.entry("كوسطة تطريز فلاحي", "Fellahi Embroidery Cushion"),
            Map.entry("صابون زيت الغار", "Laurel Oil Soap"),
            Map.entry("صابون حبة البركة", "Nigella Seed Soap"),
            Map.entry("مجسم خشبي منحوت", "Carved Wooden Sculpture"));
    private static final Map<String, String> MSG_EN = Map.ofEntries(
            Map.entry("السلة فارغة", "Your cart is empty."),
            Map.entry("البريد الإلكتروني مسجل مسبقاً", "This email is already registered."),
            Map.entry("البريد الإلكتروني غير مسجل", "Email is not registered."),
            Map.entry("البريد الإلكتروني غير صحيح", "Please enter a valid email address."),
            Map.entry("كلمة المرور غير صحيحة", "Incorrect password."),
            Map.entry("كلمة المرور ضعيفة", "Weak password: use 8+ characters with letters and numbers."),
            Map.entry("الاسم مطلوب", "Name is required."),
            Map.entry("رقم الهاتف غير صحيح، مثال: 0791234567", "Invalid phone number, e.g. 0791234567."),
            Map.entry("الفئة غير معتمدة: تطريز، صابون، خشبيات، جلوديات، سيراميك، مجوهرات", "Craft not allowed — pick one from the list."),
            Map.entry("المنتج غير موجود", "Product not found."),
            Map.entry("الكمية غير صحيحة", "Quantity is invalid."),
            Map.entry("الكمية المطلوبة غير متوفرة حالياً", "Requested quantity is out of stock."),
            Map.entry("الحد الأقصى 5 قطع من كل منتج في السلة", "You can add up to 5 pieces per product."),
            Map.entry("الحد الأدنى لقيمة الطلب 5 دنانير", "Orders start from 5 JOD."),
            Map.entry("أحد المنتجات في السلة لم يعد متوفراً", "An item in the cart is out of stock."),
            Map.entry("طريقة الدفع غير معتمدة", "Unsupported payment method."),
            Map.entry("الكود حرفة10 متاح للطلبات من 10 دنانير", "HIRFA10 applies to orders of 10 JOD or more."),
            Map.entry("الرصيد في المحفظة غير كافٍ", "Wallet balance is not enough for this order."),
            Map.entry("أقصى قيمة شحن هي 1000 دينار", "Maximum top-up is 1000 JOD."),
            Map.entry("أقل قيمة شحن هي دينار واحد", "Minimum top-up is 1 JOD."),
            Map.entry("قيمة الطلب تتجاوز حد الدفع النقدي (500 دينار)", "Cash on delivery supports orders up to 500 JOD."),
            Map.entry("رقم البطاقة غير صحيح (16 رقماً)", "Card number must be exactly 16 digits."),
            Map.entry("التقييم يجب أن يكون بين 1 و 5", "Rating must be between 1 and 5."),
            Map.entry("الحرفي غير موجود", "Artisan not found."),
            Map.entry("الفترة غير صحيحة", "Invalid period — from must be before to."),
            Map.entry("لا توجد طلبات ضمن الفترة المحددة", "No orders in the selected period."),
            Map.entry("الدفع نقداً عند التسليم", "Cash on delivery accepted."),
            Map.entry("تم تأكيد الدفع بالبطاقة", "Card payment confirmed."),
            Map.entry("تم الدفع من المحفظة", "Paid from wallet."));

    private String craftEN(String craft) {
        return craft == null ? "" : CRAFT_EN.getOrDefault(craft, craft);
    }

    private String nameEN(String name) {
        return name == null ? "" : NAME_EN.getOrDefault(name, name);
    }

    private String productEN(String name) {
        return name == null ? "" : PRODUCT_EN.getOrDefault(name, name);
    }

    private String craftValue(String label) {
        for (String c : CRAFTS) if (CRAFT_EN.get(c).equalsIgnoreCase(label)) return c;
        return label;
    }

    private String promoValue(String input) {
        if (input == null) return "";
        return switch (input.trim().toUpperCase()) {
            case "HIRFA10", "HIRFA 10" -> "حرفة10";
            case "STUDENTS5", "STUDENTS 5" -> "طلاب5";
            default -> input.trim();
        };
    }

    private String t(String m) {
        if (m == null || m.isBlank()) return "";
        String exact = MSG_EN.get(m);
        if (exact != null) return exact;
        if (m.startsWith("مرحباً بك مجدداً")) {
            String name = m.replace("مرحباً بك مجدداً", "").replace("،", "").trim();
            return "Welcome back, " + nameEN(name) + "!";
        }
        if (m.contains("تم إنشاء حسابك بنجاح")) {
            String name = m.replaceFirst("^مرحباً ?", "").replaceFirst("،.*$", "").trim();
            return "Welcome, " + nameEN(name) + "! Your account has been created.";
        }
        if (m.startsWith("مرحباً")) {
            String name = m.replaceFirst("^مرحباً ", "").replaceFirst("،.*$", "").trim();
            return "Welcome, " + nameEN(name) + "!";
        }
        if (m.startsWith("تمت إضافة ")) return "Added " + anyEN(m.replaceFirst("^تمت إضافة ", "").replaceFirst(" إلى السلة$", "")) + " to your cart.";
        if (m.startsWith("تم إنشاء الطلب رقم ")) {
            Matcher x = Pattern.compile("رقم (\\d+) بقيمة ([\\d.]+)").matcher(m);
            return anyEN(x.find() ? "Order #" + x.group(1) + " created — total " + x.group(2) + " JOD." : "Order created.");
        }
        if (m.startsWith("تم تسجيل ")) return "Registered " + nameEN(m.replaceFirst("^تم تسجيل ", "")) + " as an artisan.";
        if (m.startsWith("تم شحن المحفظة")) {
            Matcher x = Pattern.compile("([\\d.]+)").matcher(m);
            return x.find() ? "Wallet topped up — balance " + x.group(1) + " JOD." : "Wallet topped up.";
        }
        if (m.startsWith("تم إلغاء الطلب واسترداد ")) {
            Matcher x = Pattern.compile("([\\d.]+)").matcher(m);
            return x.find() ? "Order cancelled — " + x.group(1) + " JOD refunded to your wallet." : "Order cancelled with refund.";
        }
        if (m.startsWith("تم إلغاء الطلب")) return "Order cancelled.";
        if (m.startsWith("شكراً لتقييمك")) {
            Matcher x = Pattern.compile("([\\d.]+)").matcher(m);
            return x.find() ? "Thanks for rating! Average is now " + x.group(1) + "." : "Thanks for rating!";
        }
        if (m.startsWith("إيراد ")) {
            Matcher x = Pattern.compile("إيراد ([\\d.]+) دينار من (\\d+) طلب").matcher(m);
            return x.find() ? "Revenue " + x.group(1) + " JOD from " + x.group(2) + " orders." : anyEN(m);
        }
        if (m.startsWith("أصبحت حالة الطلب: ")) {
            String st = m.replaceFirst("^أصبحت حالة الطلب: ", "").replaceFirst(".$", "");
            return "Order status: " + st.replace("تم التسليم", "Delivered")
                    .replace("قيد التحضير", "Preparing").replace("قيد التوصيل", "Out for delivery") + ".";
        }
        if (m.startsWith("الكود ")) {
            String code = m.substring("الكود ".length()).replaceFirst(" غير معتمد$", "");
            return "Promo code \"" + anyEN(code) + "\" is not valid.";
        }
        return anyEN(m.startsWith("تم") ? "Success." : "Invalid input.");
    }

    private String anyEN(String x) {
        if (x == null) return "";
        String out = x;
        for (var e : PRODUCT_EN.entrySet()) out = out.replace(e.getKey(), e.getValue());
        for (var e : NAME_EN.entrySet()) out = out.replace(e.getKey(), e.getValue());
        for (var e : CRAFT_EN.entrySet()) out = out.replace(e.getKey(), e.getValue());
        out = out.replace("دينار", "JOD").replace("دنانير", "JOD")
                .replace("حرفة10", "HIRFA10").replace("طلاب5", "STUDENTS5");
        return out;
    }

    /* ---------- pages ---------- */

    private static final String CSS = """
            *{box-sizing:border-box}
            body{margin:0;background:#faf7f1;color:#33291c;font-family:'Segoe UI',Tahoma,sans-serif;line-height:1.6}
            header{background:#5d4116;color:#f5ecd9;padding:0}
            .head{max-width:1000px;margin:0 auto;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:8px;padding:12px 20px 0}
            .brand{font-size:1.25rem;font-weight:700;letter-spacing:.4px;text-decoration:none;color:#fff}
            .brand small{display:block;font-size:.62rem;font-weight:400;color:#cdb190}
            .who{display:flex;align-items:center;gap:8px;font-size:.9rem;color:#e8d9bb}
            .who .chip{background:rgba(255,255,255,.15);border-radius:99px;padding:2px 10px;color:#fff}
            nav{max-width:1000px;margin:0 auto;display:flex;flex-wrap:wrap;gap:4px;padding:10px 20px}
            nav a{color:#e8d9bb;text-decoration:none;padding:6px 12px;border-radius:8px;font-size:.92rem}
            nav a:hover{background:rgba(255,255,255,.14)}
            nav a.active{background:#8a5a00;color:#fff}
            main{max-width:1000px;margin:0 auto;padding:24px 16px 40px}
            h2{color:#5d4116;margin:0 0 4px;font-size:1.4rem}
            .sub{color:#8a7a63;font-size:.93rem;margin:0 0 18px}
            .card{background:#fff;border:1px solid #e4d8c2;border-radius:12px;padding:18px;margin-bottom:16px}
            .row{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end}
            label{display:block;font-size:.84rem;color:#8a7a63;font-weight:600;margin-bottom:4px}
            input,select{font:inherit;padding:8px 10px;border:1px solid #d3c5a8;border-radius:8px;background:#fff;color:#33291c}
            input:focus,select:focus{outline:2px solid #c9a24b}
            button{font:inherit;font-weight:700;padding:9px 16px;border:0;border-radius:8px;cursor:pointer;background:#8a5a00;color:#fff}
            button:hover{background:#6b3f00}
            button:disabled{background:#b9a98c;cursor:not-allowed}
            a.btn{display:inline-block;background:#fff;border:1px solid #8a5a00;color:#5d4116;padding:8px 14px;border-radius:8px;text-decoration:none;font-weight:600;font-size:.92rem}
            a.btn:hover{background:#f7efe0}
            .ok{background:#e6f4ea;color:#0a7d32;border:1px solid #bfe4c8;padding:10px 14px;border-radius:8px;margin:0 0 14px;font-weight:600}
            .err{background:#fdecea;color:#b3261e;border:1px solid #f0c4bf;padding:10px 14px;border-radius:8px;margin:0 0 14px;font-weight:600}
            .hint{background:#f6efe1;border:1px dashed #d3c5a8;color:#8a7a63;border-radius:8px;padding:10px 14px;font-size:.9rem}
            .grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:14px}
            .product{display:flex;flex-direction:column;gap:8px}
            .product h3{margin:0;font-size:1.02rem}
            .tag{display:inline-block;background:#e9f2ec;color:#125e38;border-radius:99px;padding:2px 10px;font-size:.78rem}
            .muted{color:#8a7a63;font-size:.88rem}
            .stock{color:#8a7a63;font-size:.85rem}
            .stock.out{color:#b3261e;font-weight:600}
            .price{font-weight:700;color:#125e38;font-size:1.12rem}
            .buy{display:flex;gap:8px;align-items:center;margin-top:auto;padding-top:8px;border-top:1px solid #f0e6d2}
            .buy input{width:52px}
            .table-wrap{overflow-x:auto}
            table{border-collapse:collapse;width:100%;background:#fff}
            th,td{padding:9px 12px;text-align:left;border-bottom:1px solid #eee2cc}
            th{background:#f4ead7;color:#5d4116;font-size:.88rem}
            .total{font-weight:700;background:#faf3e4}
            .balance{font-size:2rem;font-weight:800;color:#125e38;text-align:center;margin:6px 0 2px}
            .center{text-align:center;padding:36px 18px}
            footer{max-width:1000px;margin:0 auto;padding:0 16px 24px;color:#8a7a63;font-size:.84rem;text-align:center}
            @media(max-width:640px){.head{flex-direction:column;align-items:center}nav{justify-content:center}}
            """;

    private String header(String title, String path, Session s) {
        boolean in = s.user != null;
        String[][] links;
        if (in) links = new String[][]{{"Products", "/products"}, {"Cart", "/cart"}, {"Checkout", "/order"},
                {"Wallet", "/wallet"}, {"My Orders", "/orders"}, {"Reports", "/admin"}};
        else links = new String[][]{{"Products", "/products"}, {"Cart", "/cart"},
                {"Register", "/register"}, {"Login", "/login"}};
        StringBuilder nav = new StringBuilder();
        for (String[] l : links) {
            boolean active = (l[1].equals("/products") && (path.equals("/") || path.equals("/products"))) || l[1].equals(path);
            nav.append("<a href='").append(l[1]).append("'").append(active ? " class='active'" : "").append(">")
                    .append(l[0]).append("</a>");
        }
        String who = in
                ? "<span class='chip'>" + nameEN(s.user.name) + " · " + String.format("%.2f JOD", repo.customer(s.user.id).wallet) + "</span>"
                + "<a class='btn' href='/logout'>Log out</a>"
                : "";
        return ("<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>Hirfa — " + title + "</title><style>" + CSS + "</style></head><body>"
                + "<header><div class='head'>"
                + "<a class='brand' href='/'>Hirfa<small>handmade delivery platform</small></a>"
                + "<div class='who'>" + who + "</div></div><nav>" + nav + "</nav></header>"
                + "<main><h2>" + title + "</h2>");
    }

    private String footer() {
        return "</main><footer>Hirfa — handmade delivery platform · SE430 Software Testing Project 2026</footer></body></html>";
    }

    private String note(String enMessage, boolean ok) {
        return "<p class='" + (ok ? "ok" : "err") + "'>" + enMessage + "</p>";
    }

    /* products */

    private String products(String craftParam, String max, Session s) {
        String craft = (craftParam == null || craftParam.isBlank()) ? null : craftValue(craftParam);
        Double maxPrice = (max == null || max.isBlank()) ? null : Double.valueOf(max);
        List<Product> list = productService.search(craft, maxPrice);
        StringBuilder sb = new StringBuilder();
        sb.append(header("Products", "/products", s));
        sb.append("<p class='sub'>Handmade work from Jordanian artisans</p>");
        StringBuilder opts = new StringBuilder("<option value=''>All crafts</option>");
        for (String c : CRAFTS) {
            String label = CRAFT_EN.get(c);
            boolean chosen = label.equalsIgnoreCase(craftParam);
            opts.append("<option value='").append(label).append("'").append(chosen ? " selected" : "").append(">")
                    .append(label).append("</option>");
        }
        sb.append("""
                <div class='card'><form method='get' action='/products' class='row'>
                <div><label>Craft</label><select name='craft'>%s</select></div>
                <div><label>Max price (JOD)</label><input name='max' value='%s' size='6'></div>
                <button>Search</button> <a class='btn' href='/products'>Show all</a>
                </form></div>
                """.formatted(opts, max == null ? "" : max));
        if (list.isEmpty()) {
            sb.append("<div class='card center'>No products match your search.</div>");
        } else {
            sb.append("<div class='grid'>");
            for (Product p : list) {
                String artisanName = nameEN(repo.artisan(p.artisanId).name);
                String stock = p.stock <= 0 ? "<span class='stock out'>Out of stock</span>"
                        : "<span class='stock'>" + p.stock + " in stock</span>";
                sb.append("""
                        <div class='card product'>
                        <h3>%s</h3>
                        <span class='tag'>%s</span> <span class='muted'>by %s</span>
                        <div><span class='price'>%.2f JOD</span> — %s</div>
                        <form method='post' action='/cart' class='buy'>
                        <input type='hidden' name='productId' value='%d'>
                        <label style='margin:0'>Qty</label><input name='qty' value='1'>
                        <button %s>Add to cart</button>
                        </form></div>
                        """.formatted(productEN(p.name), craftEN(p.craft), artisanName, p.price, stock,
                        p.id, p.stock <= 0 ? "disabled" : ""));
            }
            sb.append("</div>");
        }
        return sb.toString() + footer();
    }

    /* artisan profile + rating */

    private String artisanPage(String idParam, Session s) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Artisan profile", "", s));
        if (idParam == null || idParam.isBlank()) {
            sb.append("<p class='sub'>Choose an artisan to see profile, products and rating</p><div class='card row'>");
            for (Artisan a : repo.artisans())
                sb.append("<a class='btn' href='/artisan?id=%d'>%s (%s)</a>"
                        .formatted(a.id, nameEN(a.name), craftEN(a.craft)));
            sb.append("</div>");
            return sb.toString() + footer();
        }
        int aid = Integer.parseInt(idParam);
        Artisan a = repo.artisan(aid);
        if (a == null) {
            sb.append(note(t("الحرفي غير موجود"), false));
            return sb.toString() + footer();
        }
        sb.append("<p class='sub'>").append(nameEN(a.name)).append(" — ").append(craftEN(a.craft)).append("</p>");
        sb.append("""
                <div class='card row'>
                <div><div class='balance'>%.1f <small style='font-size:.9rem;color:#8a7a63'>/ 5</small></div>
                <div class='sub' style='margin:0'>average rating</div></div>
                </div>
                """.formatted(a.rating()));
        if (s.user != null) {
            sb.append("""
                    <div class='card'><h3 style='margin:0 0 10px;font-size:1.05rem'>Rate this artisan</h3>
                    <form method='post' action='/artisan' class='row'>
                    <input type='hidden' name='id' value='%d'>
                    <div><label>Your rating</label><select name='stars'>
                    <option value='1'>1 — poor</option><option value='2'>2 — fair</option>
                    <option value='3'>3 — good</option><option value='4'>4 — very good</option>
                    <option value='5'>5 — excellent</option></select></div>
                    <button>Submit rating</button>
                    </form></div>
                    """.formatted(aid));
        } else {
            sb.append("<div class='hint'>Log in to rate this artisan.</div>");
        }
        sb.append("<h3 style='margin:16px 0 10px;font-size:1.08rem'>Products</h3><div class='grid'>");
        boolean any = false;
        for (Product p : repo.products()) {
            if (p.artisanId != aid) continue;
            any = true;
            sb.append("""
                    <div class='card product'>
                    <h3>%s</h3><span class='tag'>%s</span>
                    <div><span class='price'>%.2f JOD</span> — <span class='stock'>%d in stock</span></div>
                    </div>
                    """.formatted(productEN(p.name), craftEN(p.craft), p.price, p.stock));
        }
        if (!any) sb.append("<div class='muted'>No products yet.</div>");
        sb.append("</div>");
        return sb.toString() + footer();
    }

    private String doRateArtisan(String idParam, String starsParam, Session s) {
        int aid = Integer.parseInt(idParam);
        Outcome o = artisanService.rateArtisan(aid, Integer.parseInt(starsParam));
        StringBuilder sb = new StringBuilder();
        sb.append(header("Artisan profile", "", s));
        sb.append(note(t(o.message()), o.ok()));
        sb.append("<div class='row'><a class='btn' href='/artisan?id=").append(aid).append("'>Back to profile</a>")
                .append("<a class='btn' href='/products'>Products</a></div>");
        return sb.toString() + footer();
    }

    /* register / login / logout */

    private String registerPage(String enMessage, Session s) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Register", "/register", s));
        if (enMessage != null && !enMessage.isBlank())
            sb.append(note(enMessage, enMessage.startsWith("Welcome")));
        StringBuilder craftOpts = new StringBuilder();
        for (String c : CRAFTS)
            craftOpts.append("<option value='").append(CRAFT_EN.get(c)).append("'>").append(CRAFT_EN.get(c)).append("</option>");
        sb.append("""
                <div class='card'>
                <h3 style='margin:0 0 12px;font-size:1.05rem'>Customer account</h3>
                <form method='post' action='/register' class='row'>
                <input type='hidden' name='role' value='customer'>
                <div><label>Full name</label><input name='name' required></div>
                <div><label>Phone (07XXXXXXXXX)</label><input name='phone' required></div>
                <div><label>Email</label><input name='email' type='email' required></div>
                <div><label>Password (8+, letters and numbers)</label><input name='password' type='password' required></div>
                <button>Create account</button>
                </form></div>
                <div class='card'>
                <h3 style='margin:0 0 12px;font-size:1.05rem'>Artisan account</h3>
                <form method='post' action='/register' class='row'>
                <input type='hidden' name='role' value='artisan'>
                <div><label>Name</label><input name='name' required></div>
                <div><label>Phone</label><input name='phone' required></div>
                <div><label>Craft</label><select name='craft'>%s</select></div>
                <button>Register artisan</button>
                </form></div>
                """.formatted(craftOpts));
        return sb.toString() + footer();
    }

    private String doRegister(LinkedHashMap<String, String> params, Session s) {
        String role = params.getOrDefault("role", "customer");
        Outcome o = role.equals("artisan")
                ? artisanService.register(params.get("name"), params.get("phone"), craftValue(params.get("craft")))
                : customerService.register(params.get("name"), params.get("phone"), params.get("email"), params.get("password"));
        if (o.ok() && role.equals("customer")) {
            s.user = repo.customer((int) o.value());
            return header("Register", "/register", s) + note(t(o.message()), true)
                    + "<div class='row'><a class='btn' href='/products'>Start shopping</a>"
                    + "<a class='btn' href='/cart'>Cart</a></div>" + footer();
        }
        if (o.ok()) {
            return header("Register", "/register", s) + note(t(o.message()), true)
                    + "<div class='row'><a class='btn' href='/login'>Login</a>"
                    + "<a class='btn' href='/admin'>Reports</a></div>" + footer();
        }
        return registerPage(t(o.message()), s);
    }

    private String loginPage(String enMessage, Session s) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Login", "/login", s));
        if (enMessage != null && !enMessage.isBlank()) sb.append(note(enMessage, false));
        sb.append("""
                <div class='card' style='max-width:420px;margin:0 auto'>
                <form method='post' action='/login'>
                <label>Email</label><input name='email' type='email' required style='width:100%%;margin-bottom:10px'>
                <label>Password</label><input name='password' type='password' required style='width:100%%;margin-bottom:14px'>
                <button style='width:100%%'>Login</button>
                </form>
                <p style='margin-top:12px;font-size:.9rem;color:#8a7a63'>No account? <a href='/register'>Register</a></p>
                <div class='hint'>Demo member: amal@gmail.com / Amal@2024</div>
                </div>
                """);
        return sb.toString() + footer();
    }

    private String doLogin(LinkedHashMap<String, String> params, Session s) {
        Outcome o = customerService.login(params.get("email"), params.get("password"));
        if (o.ok()) {
            s.user = repo.customer((int) o.value());
            return header("Login", "/login", s) + note(t(o.message()), true)
                    + "<div class='row'><a class='btn' href='/products'>Products</a>"
                    + "<a class='btn' href='/cart'>Cart</a></div>" + footer();
        }
        return loginPage(t(o.message()), s);
    }

    /* cart + checkout */

    private String cartServiceSubmit(LinkedHashMap<String, String> params, Session s) {
        Outcome o = cartService.addItem(s.cart, Integer.parseInt(params.get("productId")), Integer.parseInt(params.get("qty")));
        return cartPage(o.message(), s);
    }

    private String cartPage(String rawMessage, Session s) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Cart", "/cart", s));
        sb.append("<p class='sub'>Review your items before checkout</p>");
        if (rawMessage != null && !rawMessage.isBlank())
            sb.append(note(t(rawMessage), rawMessage.startsWith("تمت")));
        var items = s.cart.all();
        if (items.isEmpty()) {
            sb.append("<div class='card center'>Your cart is empty — <a href='/products'>browse products</a>.</div>");
        } else {
            sb.append("""
                    <div class='card table-wrap'><table>
                    <thead><tr><th>Product</th><th>Qty</th><th>Unit price</th><th>Total</th></tr></thead><tbody>
                    """);
            for (var e : items.entrySet()) {
                Product p = repo.product(e.getKey());
                sb.append("<tr><td>%s</td><td>%d</td><td>%.2f JOD</td><td>%.2f JOD</td></tr>"
                        .formatted(productEN(p.name), e.getValue(), p.price, p.price * e.getValue()));
            }
            sb.append("</tbody><tfoot><tr class='total'><td colspan='3'>Cart total</td><td>%.2f JOD</td></tr></tfoot></table></div>"
                    .formatted(cartService.total(s.cart)));
        }
        sb.append("<div class='row' style='margin-top:14px'>");
        if (s.user != null) sb.append("<a class='btn' href='/order'>Checkout</a>");
        else sb.append("<a class='btn' href='/login'>Login to checkout</a>");
        sb.append("<a class='btn' href='/products'>Continue shopping</a></div>");
        return sb.toString() + footer();
    }

    private String doOrder(LinkedHashMap<String, String> params, Session s) {
        if (s.user == null) return checkoutPage("Please log in before checking out.", s);
        double km = Double.parseDouble(params.getOrDefault("km", "7"));
        Outcome o = orderService.place(s.user, s.cart, km, params.get("payment"),
                promoValue(params.getOrDefault("promo", "")), LocalDateTime.now());
        if (o.ok()) {
            s.cart.clear();
            return checkoutPage(t(o.message()) + " Estimated arrival: " + eta.estimate(km, 0) + " minutes.", s);
        }
        return checkoutPage(t(o.message()), s);
    }

    private String checkoutPage(String message, Session s) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Checkout", "/order", s));
        sb.append("<p class='sub'>Confirm delivery details and payment</p>");
        if (message != null && !message.isBlank())
            sb.append(note(message, message.startsWith("Order #")));
        if (s.user == null) {
            sb.append("<div class='card center'><a href='/login'>Login</a> or <a href='/register'>register</a> to place orders.</div>");
            return sb.toString() + footer();
        }
        var items = s.cart.all();
        sb.append("<div class='card table-wrap'><table><thead><tr><th>Item</th><th>Qty</th><th>Line total</th></tr></thead><tbody>");
        if (items.isEmpty()) sb.append("<tr><td colspan='3' class='center'>Cart is empty</td></tr>");
        for (var e : items.entrySet()) {
            Product p = repo.product(e.getKey());
            sb.append("<tr><td>%s</td><td>%d</td><td>%.2f JOD</td></tr>"
                    .formatted(productEN(p.name), e.getValue(), p.price * e.getValue()));
        }
        sb.append("<tr class='total'><td colspan='2'>Cart total</td><td>%.2f JOD</td></tr></table></div>"
                .formatted(cartService.total(s.cart)));
        sb.append("""
                <div class='card'><form method='post' action='/order' class='row'>
                <div><label>Distance (km)</label><input name='km' value='7' size='5'></div>
                <div><label>Payment</label><select name='payment'>
                <option value='CASH'>Cash on delivery</option>
                <option value='CARD'>Bank card</option>
                <option value='WALLET'>Wallet</option></select></div>
                <div><label>Promo code</label><input name='promo' placeholder='HIRFA10 / STUDENTS5'></div>
                <div><label>Card number (card only)</label><input name='card' size='18'></div>
                <button>Place the order</button>
                </form></div>
                <div class='hint'>Minimum order 5 JOD · Free delivery from 100 JOD · HIRFA10 = 10% from 10 JOD · STUDENTS5 = 5%</div>
                """);
        sb.append("<div class='row' style='margin-top:12px'><a class='btn' href='/cart'>Cart</a>"
                + "<a class='btn' href='/wallet'>Wallet</a></div>");
        return sb.toString() + footer();
    }

    /* wallet */

    private String doWallet(LinkedHashMap<String, String> params, Session s) {
        if (s.user == null) return walletPage("سجل الدخول أولاً لاستخدام المحفظة", s);
        Outcome o = walletService.topUp(s.user, Double.parseDouble(params.getOrDefault("amount", "0")));
        return walletPage(o.message(), s);
    }

    private String walletPage(String rawMessage, Session s) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Wallet", "/wallet", s));
        if (rawMessage != null && !rawMessage.isBlank())
            sb.append(note(t(rawMessage), rawMessage.startsWith("تم شحن")));
        if (s.user == null) {
            sb.append("<div class='card center'><a href='/login'>Login</a> to use the wallet.</div>");
            return sb.toString() + footer();
        }
        sb.append("<div class='card center'><div class='balance'>%.2f JOD</div><div class='sub' style='margin:0'>Current balance</div></div>"
                .formatted(repo.customer(s.user.id).wallet));
        sb.append("""
                <div class='card'><form method='post' action='/wallet' class='row'>
                <div><label>Amount (1 – 1000 JOD)</label><input name='amount' size='8'></div>
                <button>Top up</button>
                </form></div>
                """);
        return sb.toString() + footer();
    }

    /* my orders */

    private String ordersPage(Session s) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("My orders", "/orders", s));
        if (s.user == null) {
            sb.append("<div class='card center'>Log in to see your orders — <a href='/login'>Login</a>.</div>");
            return sb.toString() + footer();
        }
        List<Order> list = repo.ordersByCustomer(s.user.id);
        if (list.isEmpty()) {
            sb.append("<div class='card center'>No orders yet — <a href='/products'>start shopping</a>.</div>");
            return sb.toString() + footer();
        }
        sb.append("""
                <div class='card table-wrap'><table>
                <thead><tr><th>Order</th><th>Date</th><th>Payment</th><th>Status</th><th>Total</th></tr></thead><tbody>
                """);
        for (Order o : list) {
            sb.append("<tr><td>#%d</td><td>%s</td><td>%s</td><td><span class='tag'>%s</span></td><td>%.2f JOD</td></tr>"
                    .formatted(o.id, o.createdAt.toLocalDate(), o.payment, statusEN(o.status), o.total));
        }
        sb.append("</tbody></table></div>");
        return sb.toString() + footer();
    }

    private String statusEN(Order.Status st) {
        return switch (st) {
            case NEW -> "New";
            case PREPARING -> "Preparing";
            case SHIPPING -> "Out for delivery";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
        };
    }

    /* admin reports */

    private String adminPage(String idParam, String from, String to, Session s) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Reports", "/admin", s));
        sb.append("<p class='sub'>Revenue and units sold per artisan within a period</p>");
        StringBuilder opts = new StringBuilder();
        for (Artisan a : repo.artisans()) {
            String chosen = (idParam != null && ("" + a.id).equals(idParam)) || (idParam == null && a.id == 1) ? " selected" : "";
            opts.append("<option value='").append(a.id).append("'").append(chosen).append(">")
                    .append(nameEN(a.name)).append("</option>");
        }
        sb.append("""
                <div class='card'><form method='get' action='/admin' class='row'>
                <div><label>Artisan</label><select name='id'>%s</select></div>
                <div><label>From</label><input name='from' type='date' value='%s'></div>
                <div><label>To</label><input name='to' type='date' value='%s'></div>
                <button>Run report</button>
                </form></div>
                """.formatted(opts, from == null ? LocalDateTime.now().toLocalDate() : from,
                to == null ? LocalDateTime.now().toLocalDate() : to));
        if (idParam != null && from != null && to != null) {
            int aid = Integer.parseInt(idParam);
            Artisan a = repo.artisan(aid);
            if (a != null) {
                ReportService.ArtisanReport r = reportService.artisanReport(aid,
                        LocalDateTime.parse(from + "T00:00:00"), LocalDateTime.parse(to + "T23:59:59"));
                sb.append("""
                        <div class='card'><h3 style='margin:0 0 8px'>%s <span class='tag'>%s</span></h3>
                        <p style='margin:0'><b>%s</b></p></div>
                        """.formatted(nameEN(a.name), craftEN(a.craft), t(r.message())));
            } else {
                sb.append(note("Artisan not found.", false));
            }
        }
        return sb.toString() + footer();
    }

    private String notFound(Session s) {
        return header("Page not found", "", s)
                + "<div class='card center'>The page does not exist — <a href='/products'>back to products</a>.</div>"
                + footer();
    }

    private LinkedHashMap<String, String> parse(String body) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) map.put(urlDecode(pair.substring(0, eq)), urlDecode(pair.substring(eq + 1)));
        }
        return map;
    }

    private String urlDecode(String s) {
        return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws IOException {
        Repo repo = new Repo(args.length > 0 ? args[0] : "handmade.db");
        repo.seed();
        new WebServer(repo).start(args.length > 1 ? Integer.parseInt(args[1]) : 8080);
    }
}
