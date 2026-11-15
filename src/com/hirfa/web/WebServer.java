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
import java.util.LinkedHashMap;
import java.util.List;

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
    private final Cart demoCart = new Cart();
    private Customer signedIn;

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
        System.out.println("منصة حِرفة تعمل: http://localhost:" + port);
    }

    private void route(com.sun.net.httpserver.HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String body = ex.getRequestMethod().equals("POST") ? new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8) : "";
        String query = body.isEmpty() ? ex.getRequestURI().getRawQuery() : body;
        var params = parseForm(query == null ? "" : query);
        currentPath = path.equals("/") ? "/products" : path;
        String page;
        switch (path) {
            case "/", "/products" -> page = productsPage(params.get("craft"), params.get("max"), path);
            case "/register" -> page = ex.getRequestMethod().equals("POST")
                    ? doRegister(params) : formPage();
            case "/login" -> page = ex.getRequestMethod().equals("POST") ? doLogin(params) : loginPage("");
            case "/cart" -> page = ex.getRequestMethod().equals("POST")
                    ? doCart(params) : cartPage("");
            case "/order" -> page = ex.getRequestMethod().equals("POST")
                    ? doOrder(params) : orderPage("");
            case "/wallet" -> page = ex.getRequestMethod().equals("POST") ? doWallet(params) : walletPage("");
            case "/admin" -> page = adminPage(params.get("id"));
            default -> page = notFound();
        }
        byte[] bytes = page.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static final String CSS = """
            *{box-sizing:border-box}
            body{margin:0;background:#faf7f1;color:#33291c;font-family:'Segoe UI',Tahoma,sans-serif;line-height:1.6}
            header{background:#5d4116;color:#f5ecd9;padding:14px 20px 0}
            .head{max-width:980px;margin:0 auto;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:10px}
            .brand{font-size:1.3rem;font-weight:700;letter-spacing:.5px}
            nav{display:flex;flex-wrap:wrap;gap:4px;padding-bottom:8px}
            nav a{color:#e8d9bb;text-decoration:none;padding:6px 12px;border-radius:8px;font-size:.92rem}
            nav a:hover{background:rgba(255,255,255,.14)}
            nav a.active{background:#8a5a00;color:#fff}
            main{max-width:980px;margin:0 auto;padding:24px 16px 50px}
            h2{color:#5d4116;margin:0 0 4px;font-size:1.4rem}
            .sub{color:#8a7a63;font-size:.93rem;margin:0 0 18px}
            .card{background:#fff;border:1px solid #e4d8c2;border-radius:12px;padding:18px;margin-bottom:18px}
            .row{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-end}
            label{display:block;font-size:.85rem;color:#8a7a63;font-weight:600;margin-bottom:4px}
            input,select{font:inherit;padding:8px 10px;border:1px solid #d3c5a8;border-radius:8px;background:#fff;color:#33291c}
            input:focus,select:focus{outline:2px solid #c9a24b}
            button{font:inherit;font-weight:700;padding:9px 16px;border:0;border-radius:8px;cursor:pointer;background:#8a5a00;color:#fff}
            button:hover{background:#6b3f00}
            button:disabled{background:#b9a98c;cursor:not-allowed}
            a.btn{display:inline-block;background:#fff;border:1px solid #8a5a00;color:#5d4116;
                  padding:8px 14px;border-radius:8px;text-decoration:none;font-weight:600;font-size:.92rem}
            a.btn:hover{background:#f7efe0}
            .ok{background:#e6f4ea;color:#0a7d32;border:1px solid #bfe4c8;padding:10px 14px;border-radius:8px;margin:0 0 16px;font-weight:600}
            .err{background:#fdecea;color:#b3261e;border:1px solid #f0c4bf;padding:10px 14px;border-radius:8px;margin:0 0 16px;font-weight:600}
            .hint{background:#f6efe1;border:1px dashed #d3c5a8;color:#8a7a63;border-radius:8px;padding:10px 14px;font-size:.9rem}
            .grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:14px}
            .product{display:flex;flex-direction:column;gap:8px}
            .product h3{margin:0;font-size:1.05rem}
            .tag{display:inline-block;background:#e9f2ec;color:#125e38;border-radius:99px;padding:2px 10px;font-size:.78rem}
            .stock{color:#8a7a63;font-size:.85rem}
            .stock.out{color:#b3261e;font-weight:600}
            .price{font-weight:700;color:#125e38;font-size:1.15rem}
            .buy{display:flex;gap:8px;align-items:center;margin-top:auto;padding-top:6px;border-top:1px solid #f0e6d2}
            .buy input{width:56px}
            .table-wrap{overflow-x:auto}
            table{border-collapse:collapse;width:100%;background:#fff}
            th,td{padding:9px 12px;text-align:right;border-bottom:1px solid #eee2cc}
            th{background:#f4ead7;color:#5d4116;font-size:.88rem}
            .total{font-weight:700;background:#faf3e4}
            .balance{font-size:2rem;font-weight:800;color:#125e38;text-align:center;margin:8px 0 4px}
            .center{text-align:center;padding:42px 18px}
            footer{max-width:980px;margin:0 auto;padding:0 16px 26px;color:#8a7a63;font-size:.84rem;text-align:center}
            @media(max-width:640px){.head{flex-direction:column;align-items:center}nav{justify-content:center}}
            """;

    private String currentPath = "/";

    private String header(String title) {
        String[][] links = {{"المنتجات", "/products"}, {"تسجيل", "/register"}, {"دخول", "/login"},
                {"السلّة", "/cart"}, {"إتمام الطلب", "/order"}, {"المحفظة", "/wallet"}, {"تقارير", "/admin"}};
        StringBuilder nav = new StringBuilder();
        for (String[] l : links)
            nav.append("<a href='%s'%s>%s</a>".formatted(l[1],
                    (l[1].equals("/") || currentPath.equals(l[1])) ? " class='active'" : "", l[0]));
        return """
                <!DOCTYPE html><html lang='ar' dir='rtl'><head><meta charset='utf-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1'>
                <title>حِرفة — %s</title>
                <style>%s</style></head><body>
                <header><div class='head'><div class='brand'>حِرفة</div><nav>%s</nav></div></header>
                <main><h2>%s</h2>
                """.formatted(title, CSS, nav, title);
    }

    private String footer() {
        return "</main><footer>حِرفة — منصة توصيل الأعمال اليدوية · SE430 Software Testing Project 2026</footer></body></html>";
    }

    private String productsPage(String craft, String max, String path) {
        Double maxPrice = (max == null || max.isBlank()) ? null : Double.valueOf(max);
        List<Product> list = productService.search(craft, maxPrice);
        StringBuilder sb = new StringBuilder();
        sb.append(header("المنتجات"));
        sb.append("<p class='sub'>تصفح أعمال الحرفيين الأردنيين</p>");
        sb.append("""
                <div class='card'><form method='get' action='/products' class='row'>
                <div><label>الفئة</label><input name='craft' value='%s' placeholder='صابون'></div>
                <div><label>أقصى سعر (دينار)</label><input name='max' value='%s' size='6'></div>
                <button>بحث</button> <a class='btn' href='/products'>الكل</a>
                </form></div>
                """.formatted(craft == null ? "" : craft, max == null ? "" : max));
        if (list.isEmpty()) sb.append("<div class='card center'>لا توجد منتجات مطابقة للبحث</div>");
        else {
            sb.append("<div class='grid'>");
            for (Product p : list) {
                String stock = p.stock <= 0 ? "<span class='stock out'>غير متوفر حالياً</span>"
                        : "<span class='stock'>متوفر: " + p.stock + " قطعة</span>";
                sb.append("""
                        <div class='card product'>
                        <h3>%s</h3>
                        <span class='tag'>%s</span> <span>حرفي: %s</span>
                        <div><span class='price'>%.2f دينار</span> — %s</div>
                        <form method='post' action='/cart' class='buy'>
                        <input type='hidden' name='productId' value='%d'>
                        <label style='margin:0'>الكمية</label><input name='qty' value='1'>
                        <button %s>أضف للسلة</button>
                        </form></div>
                        """.formatted(p.name, p.craft, repo.artisan(p.artisanId).name, p.price, stock,
                        p.id, p.stock <= 0 ? "disabled" : ""));
            }
            sb.append("</div>");
        }
        return sb.toString() + footer();
    }

    private String doRegister(LinkedHashMap<String, String> params) {
        String role = params.getOrDefault("role", "customer");
        String name = params.get("name");
        Outcome out = role.equals("artisan")
                ? artisanService.register(name, params.get("phone"), params.get("craft"))
                : customerService.register(name, params.get("phone"), params.get("email"), params.get("password"));
        if (out.ok() && role.equals("customer")) signedIn = repo.customer((int) out.value());
        StringBuilder sb = new StringBuilder();
        sb.append(header("التسجيل"));
        sb.append("<p class='%s'>%s</p>".formatted(out.ok() ? "ok" : "err", out.message()));
        sb.append("<a class='btn' href='/products'>ابدأ التسوق</a>");
        return sb.toString() + footer();
    }

    private String doLogin(LinkedHashMap<String, String> params) {
        Outcome out = customerService.login(params.get("email"), params.get("password"));
        if (out.ok()) signedIn = repo.customer((int) out.value());
        StringBuilder sb = new StringBuilder();
        sb.append(header("الدخول"));
        sb.append("<p class='%s'>%s</p>".formatted(out.ok() ? "ok" : "err", out.message()));
        sb.append("<div class='row'><a class='btn' href='/products'>المنتجات</a>"
                + "<a class='btn' href='/login'>حاول مرة أخرى</a></div>");
        return sb.toString() + footer();
    }

    private String doCart(LinkedHashMap<String, String> params) {
        Outcome out = cartService.addItem(demoCart, Integer.parseInt(params.get("productId")), Integer.parseInt(params.get("qty")));
        return cartPage(out.message());
    }

    private String cartPage(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("السلّة"));
        sb.append("<p class='sub'>راجع أغراضك قبل إتمام الطلب — الحساب: %s</p>"
                .formatted(signedIn == null ? "زائر" : signedIn.name));
        if (message != null && !message.isBlank())
            sb.append("<p class='%s'>%s</p>".formatted(message.startsWith("تمت") ? "ok" : "err", message));
        var items = demoCart.all();
        if (items.isEmpty()) {
            sb.append("<div class='card center'>سلّتك فارغة — <a href='/products'>تصفح المنتجات</a></div>");
        } else {
            sb.append("<div class='card table-wrap'><table><thead><tr><th>المنتج</th><th>الكمية</th><th>سعر القطعة</th><th>المجموع</th></tr></thead><tbody>");
            for (var entry : items.entrySet()) {
                Product p = repo.product(entry.getKey());
                sb.append("<tr><td>%s</td><td>%d</td><td>%.2f دينار</td><td>%.2f دينار</td></tr>"
                        .formatted(p.name, entry.getValue(), p.price, p.price * entry.getValue()));
            }
            sb.append("</tbody><tfoot><tr class='total'><td colspan='3'>المجموع الكلي</td><td>%.2f دينار</td></tr></tfoot></table></div>"
                    .formatted(cartService.total(demoCart)));
            sb.append("""
                    <div class='card'><h3 style='margin:0 0 12px;font-size:1.05rem'>بيانات التوصيل والدفع</h3>
                    <form method='post' action='/order' class='row'>
                    <div><label>المسافة (كم)</label><input name='km' value='7' size='5'></div>
                    <div><label>طريقة الدفع</label><select name='payment'>
                    <option value='CASH'>نقداً عند التسليم</option>
                    <option value='CARD'>بطاقة بنكية</option>
                    <option value='WALLET'>المحفظة</option></select></div>
                    <div><label>كود الخصم</label><input name='promo' placeholder='حرفة10 / طلاب5'></div>
                    <div><label>رقم البطاقة (للبطاقة)</label><input name='card' size='18'></div>
                    <button>تأكيد الطلب</button>
                    </form></div>
                    <div class='hint'>الحد الأدنى للطلب 5 دنانير · الشحن مجاني من 100 دينار</div>
                    """);
        }
        sb.append("<div style='margin-top:14px'><a class='btn' href='/products'>متابعة التسوق</a></div>");
        return sb.toString() + footer();
    }

    private String doOrder(LinkedHashMap<String, String> params) {
        if (signedIn == null) return orderPage("سجل الدخول أولاً لإتمام الطلب");
        double km = Double.parseDouble(params.getOrDefault("km", "7"));
        Outcome out = orderService.place(signedIn, demoCart, km, params.get("payment"),
                params.getOrDefault("promo", ""), LocalDateTime.now());
        if (out.ok()) {
            demoCart.clear();
            return orderPage(out.message() + " — الوقت المتوقع للوصول " + eta.estimate(km, 0) + " دقيقة");
        }
        return orderPage(out.message());
    }

    private String orderPage(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("إتمام الطلب"));
        boolean ok = message == null || message.startsWith("تم إنشاء");
        if (message != null && !message.isBlank())
            sb.append("<p class='%s'>%s</p>".formatted(ok ? "ok" : "err", message));
        sb.append("<div class='row'><a class='btn' href='/products'>المنتجات</a>"
                + "<a class='btn' href='/cart'>السلّة</a><a class='btn' href='/wallet'>المحفظة</a></div>");
        return sb.toString() + footer();
    }

    private String doWallet(LinkedHashMap<String, String> params) {
        if (signedIn == null) return walletPage("سجل الدخول أولاً");
        Outcome out = walletService.topUp(signedIn, Double.parseDouble(params.getOrDefault("amount", "0")));
        return walletPage(out.message());
    }

    private String walletPage(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("المحفظة"));
        double balance = signedIn == null ? 0 : signedIn.wallet;
        if (message != null && !message.isBlank())
            sb.append("<p class='%s'>%s</p>".formatted(message.startsWith("تم شحن") ? "ok" : "err", message));
        double dbBalance = signedIn == null ? balance : repo.customer(signedIn.id).wallet;
        sb.append("""
                <div class='card center'><div class='balance'>%.2f دينار</div><div class='sub'>الرصيد الحالي</div></div>
                <div class='card'><form method='post' action='/wallet' class='row'>
                <div><label>المبلغ (1 – 1000 دينار)</label><input name='amount' size='8'></div>
                <button>شحن الرصيد</button>
                </form></div>
                """.formatted(dbBalance));
        return sb.toString() + footer();
    }

    private String adminPage(String artisanIdParam) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("تقارير الحرفيين"));
        sb.append("<p class='sub'>عدد الطلبات والإيراد لكل حرفي خلال فترة محددة</p>");
        sb.append("""
                <div class='card'><form method='get' action='/admin' class='row'>
                <div><label>رقم الحرفي</label><input name='id' value='%s' size='3'></div>
                <div><label>من</label><input name='from' value='%s'></div>
                <div><label>إلى</label><input name='to' value='%s'></div>
                <button>اعرض التقرير</button>
                </form></div>
                """.formatted(artisanIdParam == null ? "1" : artisanIdParam,
                LocalDateTime.now().toLocalDate(), LocalDateTime.now().toLocalDate()));
        if (artisanIdParam != null) {
            int aid = Integer.parseInt(artisanIdParam);
            Artisan a = repo.artisan(aid);
            if (a != null) {
                ReportService.ArtisanReport r = reportService.artisanReport(aid,
                        LocalDateTime.now().toLocalDate().atStartOfDay(), LocalDateTime.now().plusDays(1));
                sb.append("""
                        <div class='card'><h3 style='margin:0 0 8px'>%s <span class='tag'>%s</span></h3>
                        <p style='margin:0 0 10px'>%s</p>
                        <p style='margin:0'><b>عدد القطع المبيعة:</b> %d · <b>متوسط التقييم:</b> %.1f /
                        5</div>
                        """.formatted(a.name, a.craft, r.message(), r.itemsCount(), a.rating()));
            } else {
                sb.append("<p class='err'>الحرفي غير موجود</p>");
            }
        }
        return sb.toString() + footer();
    }

    private LinkedHashMap<String, String> parseForm(String body) {
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

    private String formPage() {
        StringBuilder sb = new StringBuilder();
        sb.append(header("تسجيل جديد"));
        sb.append("""
                <div class='card'>
                <h3 style='margin:0 0 12px;font-size:1.05rem'>عميل جديد</h3>
                <form method='post' action='/register' class='row'>
                <input type='hidden' name='role' value='customer'>
                <div><label>الاسم الكامل</label><input name='name' required></div>
                <div><label>الهاتف (07XXXXXXXXX)</label><input name='phone' required></div>
                <div><label>البريد الإلكتروني</label><input name='email' type='email' required></div>
                <div><label>كلمة المرور (8+ خانات)</label><input name='password' type='password' required></div>
                <button>سجّل</button>
                </form></div>
                <div class='card'>
                <h3 style='margin:0 0 12px;font-size:1.05rem'>حرفي / حرفية جديدة</h3>
                <form method='post' action='/register' class='row'>
                <input type='hidden' name='role' value='artisan'>
                <div><label>الاسم</label><input name='name' required></div>
                <div><label>الهاتف</label><input name='phone' required></div>
                <div><label>الفئة</label><select name='craft'><option>تطريز</option><option>صابون</option>
                <option>خشبيات</option><option>جلوديات</option><option>سيراميك</option><option>مجوهرات</option></select></div>
                <button>سجّل</button>
                </form></div>
                """);
        return sb.toString() + footer();
    }

    private String loginPage(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("دخول"));
        if (message != null && !message.isBlank()) sb.append("<p class='err'>").append(message).append("</p>");
        sb.append("""
                <div class='card' style='max-width:420px;margin:0 auto'>
                <form method='post' action='/login'>
                <label>البريد الإلكتروني</label><input name='email' type='email' required
                 style='width:100%;margin-bottom:10px'>
                <label>كلمة المرور</label><input name='password' type='password' required
                 style='width:100%;margin-bottom:14px'>
                <button style='width:100%'>دخول</button>
                </form>
                <p style='margin-top:12px;font-size:.9rem;color:#8a7a63'>ليس لديك حساب؟
                <a href='/register'>سجّل الآن</a></p>
                </div>
                """);
        return sb.toString() + footer();
    }

    private String notFound() {
        return header("404") + "<div class='card center'>الصفحة غير موجودة — "
                + "<a href='/products'>إلى المنتجات</a></div>" + footer();
    }

    public static void main(String[] args) throws IOException {
        Repo repo = new Repo(args.length > 0 ? args[0] : "handmade.db");
        repo.seed();
        new WebServer(repo).start(args.length > 1 ? Integer.parseInt(args[1]) : 8080);
    }
}
