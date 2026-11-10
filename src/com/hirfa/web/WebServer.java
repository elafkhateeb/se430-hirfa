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
        var params = parseForm(body.isEmpty() ? ex.getRequestURI().getRawQuery() : body);
        String page;
        switch (path) {
            case "/", "/products" -> page = productsPage(params.get("craft"), params.get("max"));
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

    private String productsPage(String craft, String max) {
        Double maxPrice = (max == null || max.isBlank()) ? null : Double.valueOf(max);
        List<Product> list = productService.search(craft, maxPrice);
        StringBuilder sb = new StringBuilder();
        sb.append(header("المنتجات"));
        sb.append("""
                <form method='get' action='/products'>
                الفئة: <input name='craft' value='%s'> أقصى سعر: <input name='max' size='5' value='%s'>
                <button>بحث</button> (<a href='/products'>الكل</a>)</form>
                <a href='/register'>تسجيل جديد</a> | <a href='/login'>دخول</a> | <a href='/cart'>السلة</a> | <a href='/order'>طلب سريع</a> | <a href='/wallet'>المحفظة</a> | <a href='/admin'>تقارير الحرفيين</a>
                <table><tr><th>المنتج</th><th>الفئة</th><th>الحرفي</th><th>السعر</th><th>المخزون</th><th></th></tr>
                """.formatted(craft == null ? "" : craft, max == null ? "" : max));
        for (Product p : list) {
            sb.append("<tr><td>%s</td><td>%s</td><td>%s</td><td>%.2f دينار</td><td>%d</td>"
                    .formatted(p.name, p.craft, repo.artisan(p.artisanId).name, p.price, p.stock));
            sb.append("""
                    <td><form method='post' action='/cart'>
                    <input type='hidden' name='productId' value='%d'> الكمية: <input name='qty' size='2' value='1'>
                    <button>أضف للسلة</button></form></td></tr>
                    """.formatted(p.id));
        }
        sb.append("</table>").append(footer());
        return sb.toString();
    }

    private String doRegister(LinkedHashMap<String, String> params) {
        String role = params.getOrDefault("role", "customer");
        String name = params.get("name");
        Outcome out = role.equals("artisan")
                ? artisanService.register(name, params.get("phone"), params.get("craft"))
                : customerService.register(name, params.get("phone"), params.get("email"), params.get("password"));
        StringBuilder sb = new StringBuilder();
        sb.append(header("التسجيل"));
        sb.append("<p class='%s'>%s</p>".formatted(out.ok() ? "ok" : "err", out.message()));
        if (out.ok() && role.equals("customer")) signedIn = repo.customer((int) out.value());
        sb.append("<a href='/register'>رجوع</a>").append(footer());
        return sb.toString();
    }

    private String doLogin(LinkedHashMap<String, String> params) {
        Outcome out = customerService.login(params.get("email"), params.get("password"));
        if (out.ok()) signedIn = repo.customer((int) out.value());
        StringBuilder sb = new StringBuilder();
        sb.append(header("الدخول"));
        sb.append("<p class='%s'>%s</p>".formatted(out.ok() ? "ok" : "err", out.message()));
        sb.append("<a href='/login'>حاول مرة أخرى</a>").append(footer());
        return sb.toString();
    }

    private String doCart(LinkedHashMap<String, String> params) {
        Outcome out = cartService.addItem(demoCart, Integer.parseInt(params.get("productId")), Integer.parseInt(params.get("qty")));
        return cartPage(out.message());
    }

    private String cartPage(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("السلة"));
        if (message != null && !message.isBlank()) sb.append("<p>%s</p>".formatted(message));
        sb.append("""
                <p>الحساب الحالي: %s</p>
                <table><tr><th>المنتج</th><th>الكمية</th></tr>
                """.formatted(signedIn == null ? "زائر" : signedIn.name));
        for (var entry : demoCart.all().entrySet()) {
            Product p = repo.product(entry.getKey());
            sb.append("<tr><td>%s</td><td>%d</td></tr>".formatted(p.name, entry.getValue()));
        }
        sb.append("</table>");
        sb.append("<p>المجموع: %.2f دينار</p>".formatted(cartService.total(demoCart)));
        sb.append("""
                <form method='post' action='/order'>
                العنوان (كم): <input name='km' size='4' value='7'>
                الدفع: <select name='payment'><option>CASH</option><option>CARD</option><option>WALLET</option></select>
                كود الخصم: <input name='promo'> رقم البطاقة (إن وجد): <input name='card'>
                <button>تأكيد الطلب</button></form><a href='/products'>متابعة التسوق</a>
                """);
        sb.append(footer());
        return sb.toString();
    }

    private String doOrder(LinkedHashMap<String, String> params) {
        if (signedIn == null) return orderPage("سجل الدخول أولاً لإتمام الطلب");
        double km = Double.parseDouble(params.getOrDefault("km", "7"));
        Outcome out = orderService.place(signedIn, demoCart, km, params.get("payment"),
                params.getOrDefault("promo", ""), LocalDateTime.now());
        if (out.ok()) {
            demoCart.clear();
            return orderPage(out.message() + " | الوقت المتوقع للوصول " + eta.estimate(km, 0) + " دقيقة");
        }
        return orderPage(out.message());
    }

    private String orderPage(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("إتمام الطلب"));
        if (message != null && !message.isBlank()) sb.append("<p>%s</p>".formatted(message));
        sb.append("<a href='/products'>رجوع للمنتجات</a>").append(footer());
        return sb.toString();
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
        if (message != null && !message.isBlank()) sb.append("<p>%s</p>".formatted(message));
        sb.append("""
                <form method='post' action='/wallet'>
                المبلغ: <input name='amount' size='6'><button>شحن</button></form>
                <p>الرصيد الحالي: %.2f دينار</p>
                """.formatted(balance));
        sb.append(footer());
        return sb.toString();
    }

    private String adminPage(String artisanIdParam) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("تقارير الحرفيين"));
        sb.append("""
                <form method='get' action='/admin'>
                رقم الحرفي: <input name='id' value='%s' size='3'>
                من: <input name='from' value='%s'> إلى: <input name='to' value='%s'>
                <button>اعرض</button></form>
                """.formatted(artisanIdParam == null ? "1" : artisanIdParam, LocalDateTime.now().toLocalDate(), LocalDateTime.now().toLocalDate()));
        if (artisanIdParam != null) {
            int aid = Integer.parseInt(artisanIdParam);
            Artisan a = repo.artisan(aid);
            if (a != null) {
                ReportService.ArtisanReport r = reportService.artisanReport(aid, LocalDateTime.now().toLocalDate().atStartOfDay(), LocalDateTime.now().plusDays(1));
                sb.append("""
                        <p>%s (%s): %s — عدد القطع المبيعة %d، متوسط التقييم %.1f</p>
                        """.formatted(a.name, a.craft, r.message(), r.itemsCount(), a.rating()));
            } else {
                sb.append("<p>الحرفي غير موجود</p>");
            }
        }
        sb.append(footer());
        return sb.toString();
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
        return header("تسجيل جديد") + """
                <h3>عميل جديد</h3>
                <form method='post' action='/register'>
                <input type='hidden' name='role' value='customer'>
                الاسم: <input name='name'> الهاتف: <input name='phone'> البريد: <input name='email'> كلمة المرور: <input name='password' type='password'>
                <button>سجّل</button></form>
                <h3>حرفي / حرفية جديدة</h3>
                <form method='post' action='/register'>
                <input type='hidden' name='role' value='artisan'>
                الاسم: <input name='name'> الهاتف: <input name='phone'>
                الفئة: <select name='craft'><option>تطريز</option><option>صابون</option><option>خشبيات</option><option>جلوديات</option><option>سيراميك</option><option>مجوهرات</option></select>
                <button>سجّل</button></form>
                """ + footer();
    }

    private String loginPage(String message) {
        return header("دخول") + "<p>" + message + "</p>" + """
                <form method='post' action='/login'>
                البريد: <input name='email'> كلمة المرور: <input name='password' type='password'>
                <button>دخول</button></form><a href='/register'>حساب جديد</a>
                """ + footer();
    }

    private String notFound() {
        return header("404") + "<p>الصفحة غير موجودة</p>" + footer();
    }

    private String header(String title) {
        return """
                <!DOCTYPE html><html lang='ar' dir='rtl'><head><meta charset='utf-8'>
                <title>حِرفة - %s</title><style>
                body{font-family:'Segoe UI',Tahoma,sans-serif;margin:24px;background:#faf7f2}
                table{border-collapse:collapse}td,th{border:1px solid #ccc;padding:6px 12px}
                th{background:#e8dcc8}.ok{color:#0a7d32}.err{color:#b3261e}
                a{color:#8a5a00}button{cursor:pointer}
                </style></head><body><h1>حِرفة — منصة توصيل الأعمال اليدوية</h1><h2>%s</h2>
                """.formatted(title, title);
    }

    private String footer() {
        return "</body></html>";
    }

    public static void main(String[] args) throws IOException {
        Repo repo = new Repo(args.length > 0 ? args[0] : "handmade.db");
        repo.seed();
        new WebServer(repo).start(args.length > 1 ? Integer.parseInt(args[1]) : 8080);
    }
}
