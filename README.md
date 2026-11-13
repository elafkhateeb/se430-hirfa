# حِرفة — منصة توصيل الأعمال اليدوية (SE430 — Software Testing Project)

نظام مطابق لمشروع مادة SE430 (خريف 2026): 20 متطلباً بوثائق BDD (Gherkin)، واختبارات صندوق أسود (تجزئة التكافؤ + 2-BVA)، واختبارات صندوق أبيض بـ JUnit 5 (تغطية العبارات والفروع)، مع قاعدة بيانات SQLite وواجهة ويب عربية.

## أعضاء الفريق

آلاء العمري، دانا حداد، رغد عنانبة، إيلاف الخطيب — جامعة العلوم والتكنولوجيا الأردنية.

## المستودع

[https://github.com/elafkhateeb/se430-hirfa](https://github.com/elafkhateeb/se430-hirfa)

## المتطلبات

- JDK 17 أو أحدث
- مكتبات `lib/` المرفقة: sqlite-jdbc، slf4j-api، junit-console، jacoco

## تشغيل النظام (واجهة ويب)

```bash
javac -cp lib/sqlite-jdbc.jar -d build $(find src -name '*.java')
java -cp "build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" com.hirfa.web.WebServer handmade.db 8080
```

ثم افتح المتصفح على `http://localhost:8080`. الصفحات: المنتجات والبحث، تسجيل عميل/حرفي، الدخول، السلة، إتمام الطلب (كاش/بطاقة/محفظة + كود خصم)، شحن المحفظة، تقرير الحرفي في `/admin?id=<رقم الحرفي>`.

قاعدة البيانات `handmade.db` تنشأ وتعبأ تلقائياً (3 حرفيين، 5 منتجات، عميلَين، أكواد الخصم: حرفة10، طلاب5).

## تشغيل اختبارات JUnit

```bash
javac -cp "build:lib/sqlite-jdbc.jar:lib/junit-console.jar" -d build $(find test src -name '*.java')
java -cp "lib/junit-console.jar:build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" org.junit.platform.console.ConsoleLauncher --scan-classpath --details=tree
```

النتيجة: 145 اختباراً ناجحاً.

## تغطية الكود (JaCoCo)

```bash
java -javaagent:lib/jacocoagent.jar=destfile=coverage/jacoco.exec \
  -cp "lib/junit-console.jar:build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" \
  org.junit.platform.console.ConsoleLauncher --scan-classpath
java -jar lib/jacococli.jar report coverage/jacoco.exec \
  --classfiles build/com/hirfa/service --classfiles build/com/hirfa/model \
  --sourcefiles src --html coverage/html --csv coverage/summary.csv
```

النتيجة على طبقات service و model:
- تغطية العبارات (Instructions): **99.7%**
- تغطية الفروع (Branches): **100%** (203/203)

## الأعضاء المختبرون لكل متطلب

| المتطلب | الدالة المختبرة | اختبار JUnit |
|---|---|---|
| 1، 2 | CustomerService.register / login | CustomerServiceTest |
| 3، 14 | ArtisanService.register / rateArtisan | ArtisanServiceTest |
| 4، 5، 6 | ProductService.add / restock / search | ProductServiceTest |
| 7، 17 | CartService.addItem / changeQty | CartServiceTest |
| 8، 16 | OrderService.place | OrderServiceTest |
| 9 | DeliveryFeeCalculator.fee | DeliveryFeeCalculatorTest |
| 10 | DiscountCalculator.apply | DiscountCalculatorTest |
| 11 | PaymentService.pay | PaymentServiceTest |
| 12، 13 | OrderLifecycleService.cancel / advance | OrderLifecycleServiceTest |
| 15، 20 | WalletService.topUp / LoyaltyService.tierFor | WalletAndLoyaltyTest |
| 18 | EtaCalculator.estimate | EtaCalculatorTest |
| 19 | ReportService.artisanReport | ReportServiceTest |

## الوثيقة المطلوب رفعها على الإي-ليرننج

`doc/SE430-وثيقة-المتطلبات-والاختبارات.html` — افتحها في المتصفح ثم اطبعها PDF (أسماء الفريق في صفحة الغلاف). نسخة PDF جاهزة للرفع: `doc/SE430-وثيقة-المتطلبات-والاختبارات.pdf` (42 صفحة، A4). الخطوط العربية محلية في `doc/fonts/`، لذا احتفظ بالمجلد بجوار ملف HTML. سيناريوهات Gherkin موجودة أيضاً في `doc/features/R01..R20.feature`. لإعادة توليد الوثيقة بعد تعديل البيانات: `python3 gen_doc.py`.
