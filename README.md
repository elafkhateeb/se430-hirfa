# حِرفة — منصة توصيل الأعمال اليدوية | SE430 Software Testing Project

<div dir="rtl">

مشروع مادة SE430 — اختبار البرمجيات، جامعة العلوم والتكنولوجيا الأردنية، خريف 2026.
منصة «حِرفة» تربط الحرفيين الأردنيين بالمشترين مع توصيل للمنتجات اليدوية (تطريز، صابون، خشبيات، مجوهرات)،
وقد بُنيت بالكامل لتكون موضوعاً لممارسة اختبارات الصندوق الأسود والصندوق الأبيض كما هو مطلوب في المشروع.

| اختبارات JUnit | تغطية الفروع | تغطية العبارات | المتطلبات |
|:---:|:---:|:---:|:---:|
| 145/145 ناجحة ✔ | 100% (203/203) | 99.7% | 20 BDD |

## أعضاء الفريق

| العضو | الدور |
|---|---|
| إيلاف الخطيب | خدمات الحرفيين، رسوم التوصيل، ملفات Gherkin، تقرير التغطية |
| دانا حداد | طبقة القاعدة والنماذج، السلة، الدفع، الأدوات وأوراق PDF |
| رغد عنانبة | المنتجات والخصومات، تقدير الوقت، الوثيقة، التقارير |
| آلاء العمري | الدخول والطلبات، دورة حياة الطلب، المحفظة والولاء، واجهة الويب و README |

## ماذا يتضمن المستودع؟

| المجلد/الملف | الوصف |
|---|---|
| `src/com/hirfa/model` | نماذج المجال: Customer, Artisan, Product, Cart, Order, OrderItem, Outcome |
| `src/com/hirfa/repo` | طبقة الوصول لقاعدة البيانات SQLite مع بيانات أولية عربية |
| `src/com/hirfa/service` | منطق العمل: التسجيل، المنتجات، السلة، الطلبات، الرسوم، الخصم، الدفع، الولاء، التقارير |
| `src/com/hirfa/web` | خادم ويب مدمج بواجهة عربية (لا يحتاج أي إطار عمل خارجي) |
| `test/com/hirfa` | اختبارات JUnit 5 لكل متطلب من المتطلبات العشرين |
| `doc/features` | سيناريوهات Gherkin — `R01.feature` حتى `R20.feature` |
| `doc` | وثيقة التسليم: HTML التفاعلي و PDF جاهز للرفع على الإي-ليرننج |
| `coverage` | تقرير JaCoCo التفصيلي (HTML + CSV) |
| `lib` | مكتبات مرفقة: sqlite-jdbc, slf4j, junit-console, jacoco CLI/agent |
| `gen_doc.py` | مولّد وثيقة التسليم (يقرأ البيانات من `docgen_data.py` و `docgen_tables.py`) |

## المتطلبات التقنية

- JDK 17 أو أحدث (لا حاجة لأي أداة بناء خارجية)
- لمكتبات في `lib/` — تم إرفاقها جميعاً في المستودع

## تشغيل النظام (واجهة الويب)

```bash
javac -cp lib/sqlite-jdbc.jar -d build $(find src -name '*.java')
java -cp "build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" com.hirfa.web.WebServer handmade.db 8080
```

ثم افتح المتصفح على: `http://localhost:8080`

| الصفحة | ما فيها |
|---|---|
| `/` و `/products` | استعراض المنتجات والبحث بالفئة وأقصى سعر |
| `/register` | تسجيل عميل جديد أو حرفي |
| `/login` | تسجيل الدخول |
| `/cart` | إضافة منتجات وتعديل الكميات (1–5 لكل منتج) |
| `/order` | إتمام الطلب: كاش/بطاقة/محفظة + كود خصم (حرفة10، طلاب5) |
| `/wallet` | شحن المحفظة (حتى 1000 دينار) |
| `/admin?id=1` | تقرير مبيعات حرفي (عدد الطلبات والإيراد) |

قاعدة البيانات `handmade.db` تُنشأ وتُعبأ تلقائياً عند أول تشغيل:
3 حرفيين (سمية المجالي، ليان الزعبي، محمد العمري)، 5 منتجات، عميلان (أمل الخطيب، ريم الشوبكي)، وأكواد الخصم.

## تشغيل اختبارات JUnit

```bash
javac -cp "build:lib/sqlite-jdbc.jar:lib/junit-console.jar" -d build $(find test src -name '*.java')
java -cp "lib/junit-console.jar:build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" \
  org.junit.platform.console.ConsoleLauncher --scan-classpath --details=tree
```

النتيجة: **145 اختباراً ناجحاً، صفر إخفاقات.**

## قياس التغطية (JaCoCo)

```bash
java -javaagent:lib/jacocoagent.jar=destfile=coverage/jacoco.exec \
  -cp "lib/junit-console.jar:build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" \
  org.junit.platform.console.ConsoleLauncher --scan-classpath

java -jar lib/jacococli.jar report coverage/jacoco.exec \
  --classfiles build/com/hirfa/service --classfiles build/com/hirfa/model \
  --sourcefiles src --html coverage/html --csv coverage/summary.csv
```

النتيجة على طبقتَي `service` و `model`:

| المقياس | القيمة |
|---|---|
| تغطية العبارات (Instructions) | 99.7% |
| تغطية الفروع (Branches) | **100%** — 203 فرعاً مغطاة بالكامل |

التقرير التفاعلي مع خرائط تغطية كل سطر: `coverage/html/index.html`

## مصفوفة التتبع: المتطلب → الشيفرة → الاختبار

| المتطلبات | الدالة المختبرة | اختبار JUnit |
|---|---|---|
| 1، 2 | `CustomerService.register` / `login` | `CustomerServiceTest` |
| 3، 14 | `ArtisanService.register` / `rateArtisan` | `ArtisanServiceTest` |
| 4، 5، 6 | `ProductService.add` / `restock` / `search` | `ProductServiceTest` |
| 7، 17 | `CartService.addItem` / `changeQty` | `CartServiceTest` |
| 8، 16 | `OrderService.place` | `OrderServiceTest` |
| 9 | `DeliveryFeeCalculator.fee` | `DeliveryFeeCalculatorTest` |
| 10 | `DiscountCalculator.apply` | `DiscountCalculatorTest` |
| 11 | `PaymentService.pay` | `PaymentServiceTest` |
| 12، 13 | `OrderLifecycleService.cancel` / `advance` | `OrderLifecycleServiceTest` |
| 15، 20 | `WalletService.topUp` / `LoyaltyService.tierFor` | `WalletAndLoyaltyTest` |
| 18 | `EtaCalculator.estimate` | `EtaCalculatorTest` |
| 19 | `ReportService.artisanReport` | `ReportServiceTest` |

## منهجية الاختبار

- **صندوق أسود — تجزئة التكافؤ (EP):** تقسيم مدخلات كل متطلب إلى فئات متكافئة (صحيحة/فارغة/خاطئة/فوق الحد) مع حالة اختبار لكل فئة.
- **صندوق أسود — قيم الحدود (2-BVA):** لكل حد رقمي حالة عند الحد وقبل الحد وبعده (مثل الإلغاء عند 119/120/121 دقيقة، السعر عند 0.01/1000.01).
- **صندوق أبيض — تغطية العبارات:** حالة اختبار تضمن تنفيذ كل عبارة في الدالة.
- **صندوق أبيض — تغطية الفروع:** كل قرار (if/switch/حلقة) يُختبر بطرفيه الصادق والكاذب، وتم التحقق منها آلياً حتى 100% في الفروع.

تفاصيل كل الجداول الأربعة لكل متطلب في وثيقة التسليم أدناه.

## وثيقة التسليم (E-Learning)

- العرض التفاعلي: `doc/SE430-وثيقة-المتطلبات-والاختبارات.html`
- نسخة PDF جاهزة للرفع: `doc/SE430-وثيقة-المتطلبات-والاختبارات.pdf`
- سيناريوهات Gherkin لكل متطلب: `doc/features/R01..R20.feature`

لإعادة توليد الوثيقة بعد تعديل البيانات:

```bash
python3 gen_doc.py
```

</div>
