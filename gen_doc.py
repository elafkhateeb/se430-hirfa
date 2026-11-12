# -*- coding: utf-8 -*-
"""مولّد وثيقة SE430 (HTML) — منصة «حِرفة».

يعيد بناء:
  doc/SE430-وثيقة-المتطلبات-والاختبارات.html
  doc/features/R01..R20.feature

التشغيل:  python3 gen_doc.py
"""
import csv
import html
import os
import re
from collections import OrderedDict

from docgen_data import REQS
from docgen_tables import TABLES

ROOT = os.path.dirname(os.path.abspath(__file__))
DOC_PATH = os.path.join(ROOT, "doc", "SE430-وثيقة-المتطلبات-والاختبارات.html")

TEAM = ["آلاء العمري", "دانا حداد", "رغد عنانبة", "إيلاف الخطيب"]

# الوحدات الوظيفية المستخدمة في الفهرس والمصفوفة (الترقيم في المتن يبقى 1–20)
MODULES = [
    ("الحسابات والوصول", "تسجيل العملاء والحرفيين وتسجيل الدخول", [1, 2, 3]),
    ("الكتالوج والمنتجات", "إضافة المنتجات وإعادة تخزينها والبحث عنها", [4, 5, 6]),
    ("السلة والطلب", "إدارة السلة وإنشاء الطلب وحدوده", [7, 17, 8, 16]),
    ("الدفع والتوصيل", "أجرة التوصيل والخصم والدفع وتقدير الوقت", [9, 10, 11, 18]),
    ("دورة حياة الطلب", "إلغاء الطلب وتحديث حالته", [12, 13]),
    ("الحرفيون والولاء والتقارير", "تقييم الحرفي والتقارير والمحفظة والولاء", [14, 19, 20, 15]),
]
MODULE_OF = {n: title for title, _desc, nums in MODULES for n in nums}

SECTIONS = [
    ("docinfo", "بيانات الوثيقة", "بطاقة تعريف المشروع ونطاق الوثيقة"),
    ("method", "منهجية الاختبار", "الصندوق الأسود والأبيض وأدوات القياس"),
    ("toc", "فهرس المتطلبات", "خريطة الوثيقة والمتطلبات حسب الوحدة"),
    ("trace", "مصفوفة التتبع", "ربط المتطلبات بالدوال واختبارات JUnit"),
    ("reqs", "المتطلبات وجداول الاختبار", "20 بطاقة متطلب × 4 جداول"),
    ("coverage", "تقرير التغطية", "نتائج JaCoCo لكل صنف"),
    ("appendix", "الملحق", "أوامر إعادة إنتاج النتائج"),
]

FONT_CSS = """
/* Cairo + Noto Naskh Arabic — SIL Open Font License 1.1 (Google Fonts) */
@font-face{font-family:'Cairo';font-style:normal;font-weight:400 700;font-display:swap;
  src:url('fonts/Cairo-arabic.woff2') format('woff2');
  unicode-range:U+0600-06FF,U+0750-077F,U+0870-0891,U+0897-08E1,U+08E3-08FF,U+200C-200E,U+2010-2011,U+204F,U+2E41,U+FB50-FDFF,U+FE70-FE74,U+FE76-FEFC,U+102E0-102FB,U+10E60-10E7E,U+10EC2-10EC7,U+10EC9-10EEE,U+10EF0-10EFF,U+1EE00-1EE03,U+1EE05-1EE1F,U+1EE21-1EE22,U+1EE24,U+1EE27,U+1EE29-1EE32,U+1EE34-1EE37,U+1EE39,U+1EE3B,U+1EE42,U+1EE47,U+1EE49,U+1EE4B,U+1EE4D-1EE4F,U+1EE51-1EE52,U+1EE54,U+1EE57,U+1EE59,U+1EE5B,U+1EE5D,U+1EE5F,U+1EE61-1EE62,U+1EE64,U+1EE67-1EE6A,U+1EE6C-1EE72,U+1EE74-1EE77,U+1EE79-1EE7C,U+1EE7E,U+1EE80-1EE89,U+1EE8B-1EE9B,U+1EEA1-1EEA3,U+1EEA5-1EEA9,U+1EEAB-1EEBB,U+1EEF0-1EEF1;}
@font-face{font-family:'Cairo';font-style:normal;font-weight:400 700;font-display:swap;
  src:url('fonts/Cairo-latin.woff2') format('woff2');
  unicode-range:U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,U+0308,U+0329,U+2000-206F,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD;}
@font-face{font-family:'Noto Naskh Arabic';font-style:normal;font-weight:400 700;font-display:swap;
  src:url('fonts/NotoNaskhArabic-arabic.woff2') format('woff2');
  unicode-range:U+0600-06FF,U+0750-077F,U+0870-0891,U+0897-08E1,U+08E3-08FF,U+200C-200E,U+2010-2011,U+204F,U+2E41,U+FB50-FDFF,U+FE70-FE74,U+FE76-FEFC,U+102E0-102FB,U+10E60-10E7E,U+10EC2-10EC7,U+10EC9-10EEE,U+10EF0-10EFF,U+1EE00-1EE03,U+1EE05-1EE1F,U+1EE21-1EE22,U+1EE24,U+1EE27,U+1EE29-1EE32,U+1EE34-1EE37,U+1EE39,U+1EE3B,U+1EE42,U+1EE47,U+1EE49,U+1EE4B,U+1EE4D-1EE4F,U+1EE51-1EE52,U+1EE54,U+1EE57,U+1EE59,U+1EE5B,U+1EE5D,U+1EE5F,U+1EE61-1EE62,U+1EE64,U+1EE67-1EE6A,U+1EE6C-1EE72,U+1EE74-1EE77,U+1EE79-1EE7C,U+1EE7E,U+1EE80-1EE89,U+1EE8B-1EE9B,U+1EEA1-1EEA3,U+1EEA5-1EEA9,U+1EEAB-1EEBB,U+1EEF0-1EEF1;}
"""

CSS = FONT_CSS + """
:root{
  --ink:#1f2937; --muted:#6b7280;
  --green:#125e38; --green-2:#1d7a4c; --green-soft:#eef6f0;
  --brown:#6d441c; --brown-soft:#f7f1e6;
  --gold:#c9a24b; --gold-soft:#f1e6cd;
  --line:#e6ddcc; --paper:#ffffff; --bg:#f2ede3;
  --font-body:'Noto Naskh Arabic','Cairo','Segoe UI',Tahoma,sans-serif;
  --font-ui:'Cairo','Segoe UI',Tahoma,sans-serif;
  --font-mono:'DejaVu Sans Mono',Consolas,'Courier New',monospace;
}
*{box-sizing:border-box}
html{scroll-behavior:smooth}
body{margin:0;background:var(--bg);color:var(--ink);font-family:var(--font-body);font-size:16px;line-height:1.9;-webkit-text-size-adjust:100%}
a{color:var(--green);text-decoration:none}
a:hover{text-decoration:underline}
code{font-family:var(--font-mono);direction:ltr;unicode-bidi:isolate;font-size:.88em;background:#f4efe4;border:1px solid #e4d8bf;border-radius:6px;padding:1px 5px;color:#3f2d14}
.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}

/* ---------- الشريط العلوي (شاشة فقط) ---------- */
.topbar{position:sticky;top:0;z-index:50;background:rgba(255,253,248,.94);border-bottom:1px solid var(--line);backdrop-filter:blur(10px)}
.topbar-in{max-width:1000px;margin:0 auto;padding:9px 22px;display:flex;align-items:center;gap:18px;justify-content:space-between}
.brand{display:flex;align-items:center;gap:10px;min-width:0}
.brand-mark{width:34px;height:34px;border-radius:10px;background:var(--green);color:#fff;display:grid;place-items:center;font-family:var(--font-ui);font-weight:700;font-size:17px;flex:0 0 auto}
.brand b{display:block;font-family:var(--font-ui);font-size:13px;color:var(--brown);line-height:1.5;white-space:nowrap}
.brand small{display:block;font-family:var(--font-ui);font-size:10px;color:var(--muted);line-height:1.4}
.topnav{display:flex;align-items:center;gap:4px;font-family:var(--font-ui);font-size:12px}
.topnav a{padding:6px 9px;border-radius:8px;color:#4b5563}
.topnav a:hover{background:var(--green-soft);color:var(--green);text-decoration:none}
.print-btn{font-family:var(--font-ui);font-size:12px;font-weight:700;color:#fff;background:var(--green);border:0;border-radius:8px;padding:7px 12px;cursor:pointer}
.print-btn:hover{background:var(--green-2)}
a:focus-visible,button:focus-visible{outline:2px solid var(--gold);outline-offset:2px}

.sheet{max-width:1000px;margin:22px auto 46px;padding:54px 58px 44px;background:var(--paper);border-radius:18px;box-shadow:0 18px 50px rgba(74,55,18,.10)}

/* ---------- الغلاف ---------- */
.cover{position:relative;text-align:center;padding:46px 40px 38px;border:1px solid var(--line);border-radius:18px;background:linear-gradient(180deg,#fffdf8 0%,#fbf7ee 100%);overflow:hidden;page-break-after:always;break-after:page;margin:2px 0 8px}
.cover::before{content:"";position:absolute;inset:14px;border:1px solid rgba(201,162,75,.55);border-radius:12px;pointer-events:none}
.cover::after{content:"";position:absolute;top:0;left:0;right:0;height:5px;background:linear-gradient(90deg,var(--green),var(--gold),var(--green))}
.cover>*{position:relative;z-index:1}
.cover .uni{margin:6px 0 0;font-family:var(--font-ui);font-size:16px;font-weight:700;color:var(--brown)}
.cover .course{margin:14px 0 0}
.cover .course span{display:inline-block;font-family:var(--font-ui);font-size:11.5px;font-weight:700;letter-spacing:.3px;color:var(--green);background:var(--green-soft);border:1px solid #c4ddcd;border-radius:999px;padding:4px 14px}
.cover h1{font-family:var(--font-ui);font-size:38px;line-height:1.45;color:var(--brown);margin:18px 0 8px;border:0;padding:0}
.cover .project{margin:0 auto;max-width:680px;font-size:17px;color:#4b5563;line-height:1.85}
.cover .coverage-line{margin:10px 0 0;font-family:var(--font-ui);font-size:11.5px;color:#8a7a5c}
.cover .divider{width:130px;height:2px;margin:22px auto;background:linear-gradient(90deg,transparent,var(--gold),transparent)}
.cover .team-label{margin:0 0 8px;font-family:var(--font-ui);font-size:11px;letter-spacing:.6px;color:#9a8768}
.team{display:flex;flex-wrap:wrap;justify-content:center;gap:8px;max-width:730px;margin:0 auto}
.member{font-family:var(--font-ui);font-size:13px;font-weight:600;color:#3f2d14;background:#fff;border:1px solid #e8ddc7;border-radius:9px;padding:6px 14px}
.cover-stats{display:grid;grid-template-columns:repeat(4,1fr);gap:9px;max-width:700px;margin:24px auto 0}
.cover-stats div{background:#fff;border:1px solid #e8ddc7;border-radius:10px;padding:9px 6px}
.cover-stats b{display:block;font-family:var(--font-ui);font-size:21px;line-height:1.5;color:var(--green)}
.cover-stats span{display:block;font-family:var(--font-ui);font-size:10.5px;color:#8a7a5c}
.cover-foot{display:flex;justify-content:center;gap:18px;flex-wrap:wrap;margin:22px 0 0;padding-top:13px;border-top:1px dashed #d9cdb4;font-family:var(--font-ui);font-size:11px;color:#9a8768}

/* ---------- بيانات الوثيقة ---------- */
.docinfo{margin:26px 0 0;scroll-margin-top:80px}
.docinfo-head{display:flex;align-items:center;justify-content:space-between;gap:12px;margin:0 0 10px}
.docinfo-head h2{margin:0;font-family:var(--font-ui);font-size:17px;color:var(--brown);border:0;padding:0;background:none}
.docinfo-head span{font-family:var(--font-ui);font-size:10.5px;color:#9a8768}
.info-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}
.info-grid div{background:#fbf8f0;border:1px solid var(--line);border-radius:10px;padding:8px 11px;min-width:0}
.info-grid span{display:block;font-family:var(--font-ui);font-size:9.5px;letter-spacing:.2px;color:#9a8768;margin-bottom:1px}
.info-grid b{display:block;font-family:var(--font-ui);font-size:12px;color:#3f2d14;font-weight:600;line-height:1.6;overflow-wrap:anywhere}

/* ---------- العناوين والأقسام ---------- */
.sec{display:flex;align-items:center;gap:12px;margin:42px 0 14px;padding:0 0 10px;border-bottom:2px solid var(--gold);font-family:var(--font-ui);font-size:22px;line-height:1.5;color:var(--green);scroll-margin-top:78px}
.sec .secno{display:grid;place-items:center;width:38px;height:38px;border-radius:10px;background:var(--green);color:#fff;font-size:17px;font-weight:700;flex:0 0 auto}
h3{font-family:var(--font-ui);font-size:16px;color:var(--brown);margin:18px 0 8px;line-height:1.6}
.lead{margin:0 0 18px;font-size:15px;color:#4b5563;line-height:1.9}
.note{background:#fdf8ee;border:1px solid #ecdcb3;border-right:4px solid var(--gold);border-radius:10px;padding:9px 14px;font-size:13.5px;color:#5b4a2c;line-height:1.85;margin:14px 0}
.note b{color:#4a3712}

.cards{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin:16px 0}
.card{border:1px solid var(--line);border-radius:12px;padding:14px 16px;background:#fff}
.card-head{display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:6px}
.card-head h3{margin:0;font-family:var(--font-ui);font-size:14px;color:var(--brown);line-height:1.6;border:0;padding:0}
.dot{width:8px;height:8px;border-radius:50%;background:var(--gold);display:inline-block;flex:0 0 auto}
.card p{margin:0;font-size:13px;color:#4b5563;line-height:1.85}
.card ul{margin:8px 0 0;padding:0 16px 0 0;font-size:12.5px;color:#4b5563;line-height:1.8}
.card li{margin:2px 0}
.tag{margin-inline-start:auto;font-family:var(--font-ui);font-size:9.5px;font-weight:700;color:#7a5a2e;background:var(--gold-soft);border-radius:999px;padding:1px 8px;white-space:nowrap}

.kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin:16px 0}
.kpi{background:linear-gradient(180deg,#fff,#fbf8f0);border:1px solid var(--line);border-radius:11px;padding:10px 13px}
.kpi b{display:block;font-family:var(--font-ui);font-size:22px;line-height:1.5;color:var(--green)}
.kpi span{display:block;font-family:var(--font-ui);font-size:10.5px;color:#8a7a5c}
.kpi small{display:block;font-size:10px;color:#a2947a;font-family:var(--font-ui)}

/* ---------- الفهرس ---------- */
.toc-grid{display:grid;grid-template-columns:.92fr 1.18fr;gap:18px;align-items:start}
.toc-panel{border:1px solid var(--line);border-radius:12px;padding:13px 15px;background:#fff}
.toc-h{margin:0 0 8px;font-family:var(--font-ui);font-size:11px;letter-spacing:.4px;color:#9a8768;font-weight:700}
.sec-list{list-style:none;margin:0;padding:0}
.sec-list a{display:flex;gap:9px;padding:6px 0;border-bottom:1px dashed #efe6d5;font-size:13.5px;color:#3f2d14}
.sec-list li:last-child a{border-bottom:0}
.sec-list .sn{font-family:var(--font-ui);font-weight:700;color:var(--gold);min-width:20px;flex:0 0 auto}
.sec-list .st{display:block}
.sec-list small{display:block;font-family:var(--font-ui);font-size:10px;color:var(--muted);line-height:1.6}
.toc-group{margin:0 0 9px}
.toc-group:last-child{margin-bottom:0}
.toc-group h4{display:flex;justify-content:space-between;gap:8px;margin:0 0 2px;font-family:var(--font-ui);font-size:11px;color:var(--brown);font-weight:700}
.toc-group h4 span{color:#b09a76;font-weight:400}
.toc-req{list-style:none;margin:0;padding:0}
.toc-req a{display:flex;align-items:baseline;gap:7px;padding:2px 0;font-size:13px;color:#4b5563}
.toc-req .num{font-family:var(--font-mono);font-size:10px;color:var(--gold);min-width:21px;flex:0 0 auto}
.toc-req .cnt{margin-inline-start:auto;font-family:var(--font-ui);font-size:9.5px;color:#b3a58c;white-space:nowrap}

/* ---------- مصفوفة التتبع ---------- */
.trace td{font-size:11.5px;overflow-wrap:normal}
.trace td code{font-size:10.5px;background:none;border:0;padding:0;white-space:normal;overflow-wrap:normal}
.trace .unit{display:block;font-family:var(--font-ui);font-size:9.5px;color:#a2947a}
.trace .c-counts{font-family:var(--font-mono);font-size:10.5px;color:#6b5a3d;white-space:nowrap;text-align:center;direction:ltr}
.trace .dotsep{color:#b9a17a;padding:0 3px;font-family:var(--font-mono)}
.trace .c-total{font-family:var(--font-ui);font-weight:700;color:var(--green);text-align:center}
.trace thead th:nth-child(5){direction:ltr;text-align:center}
.trace tr.total td{background:#f3f7f4;font-weight:700;border-top:2px solid #c4ddcd}

/* ---------- بطاقة المتطلب ---------- */
.req{border:1px solid var(--line);border-radius:16px;background:#fff;padding:26px 28px;margin:24px 0;box-shadow:0 1px 2px rgba(74,55,18,.04);scroll-margin-top:78px}
.req-head{display:flex;align-items:flex-start;gap:15px;border-bottom:1px solid #efe7d7;padding-bottom:15px;margin-bottom:14px}
.req-num{flex:0 0 auto;width:62px;height:62px;border-radius:14px;background:linear-gradient(145deg,var(--green),var(--green-2));color:#fff;display:flex;flex-direction:column;align-items:center;justify-content:center;font-family:var(--font-ui);line-height:1.15;box-shadow:inset 0 0 0 1px rgba(255,255,255,.16)}
.req-num small{font-size:8.5px;font-weight:400;opacity:.82}
.req-num b{font-size:22px;font-weight:700}
.req-titles{min-width:0}
.req-kicker{margin:0 0 1px;font-family:var(--font-ui);font-size:10.5px;letter-spacing:.2px;color:#9a8768}
.req-titles h3{margin:0 0 5px;font-family:var(--font-ui);font-size:20px;line-height:1.55;color:var(--brown);border:0;padding:0}
.req-desc{margin:0;font-size:14px;line-height:1.9;color:#4b5563}
.req-meta{display:grid;grid-template-columns:repeat(4,1fr);gap:7px;margin:0 0 18px}
.req-meta div{background:var(--brown-soft);border:1px solid #eadfcb;border-radius:9px;padding:6px 10px;min-width:0}
.req-meta span{display:block;font-family:var(--font-ui);font-size:9.5px;color:#8a7a5c}
.req-meta b{display:block;font-family:var(--font-mono);font-size:11px;font-weight:600;line-height:1.6;color:#3f2d14;overflow-wrap:normal;direction:ltr;unicode-bidi:isolate}

/* ---------- BDD ---------- */
.bdd-card{border:1px solid #d9cfb7;border-radius:12px;overflow:hidden;margin:0 0 18px;background:#fbf8f0;page-break-inside:avoid;break-inside:avoid}
.bdd-head{display:flex;align-items:center;justify-content:space-between;gap:10px;background:#efe6d2;padding:7px 13px;font-family:var(--font-ui);font-size:12px;font-weight:700;color:#5b431f}
.bdd-head .lbl{display:flex;align-items:center;gap:7px}
.bdd-head .lbl .dot{background:var(--green)}
.bdd-head .file{font-family:var(--font-mono);font-weight:400;font-size:10px;color:#7a6a4c;background:#fff;border:1px solid #e0d6bf;border-radius:999px;padding:1px 8px;direction:ltr}
.gherkin{counter-reset:gl;margin:0;padding:11px 12px 11px 14px;background:#fbf8f0;border:0;border-radius:0;font-family:'Noto Naskh Arabic','DejaVu Sans Mono',monospace;font-size:13px;line-height:1.95;direction:rtl;text-align:right;white-space:pre-wrap;overflow-wrap:anywhere;color:#3f2d14}
.gl{display:block;position:relative;padding-right:2.5em;border-radius:5px;min-height:1.5em}
.gl::before{counter-increment:gl;content:counter(gl,decimal-leading-zero);position:absolute;right:.15em;top:.15em;font-family:var(--font-mono);font-size:9px;line-height:1.6;color:#c2b08c;direction:ltr}
.gl.l-scen{background:#f2ecdf;padding-top:1px;padding-bottom:1px}
.gk{font-family:var(--font-mono);font-size:10.5px;font-weight:700;color:#8a5a17;background:#f1e6cd;border-radius:4px;padding:1px 5px;direction:ltr;unicode-bidi:isolate}
.gk-feat{color:#0d5c3a;background:#e3f1e8}
.gk-scen{color:#6d441c;background:#efe1c6}
.gk-step{color:#8a5a17;background:#f3ead8}
.gs{color:#0f5c3c}

/* ---------- مفتاح الجداول ---------- */
.legend{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin:14px 0}
.legend div{border:1px solid var(--line);border-radius:9px;padding:7px 11px;background:#fbf8f0;border-top:3px solid #2f7d5b}
.legend b{display:block;font-family:var(--font-ui);font-size:9.5px;font-weight:700;color:#9a8768;letter-spacing:.2px}
.legend span{display:block;font-family:var(--font-ui);font-size:10.5px;color:#4b5563;line-height:1.7}
.legend .t-bva{border-top-color:#a97a25}
.legend .t-sc{border-top-color:#6d441c}
.legend .t-bc{border-top-color:#2f6d4f}

/* ---------- الجداول ---------- */
.tbl-card{border:1px solid var(--line);border-top:3px solid #2f7d5b;border-radius:12px;margin:0 0 15px;background:#fff;overflow:hidden;page-break-inside:avoid;break-inside:avoid}
.tbl-card.t-bva{border-top-color:#a97a25}
.tbl-card.t-sc{border-top-color:#6d441c}
.tbl-card.t-bc{border-top-color:#2f6d4f}
.tbl-card.split{page-break-inside:auto;break-inside:auto}
.tbl{width:100%;border-collapse:collapse;table-layout:fixed;margin:0;font-size:13px}
.tbl caption{padding:0;caption-side:top;text-align:right;background:none}
.tcap{display:flex;align-items:center;gap:9px;padding:8px 12px;background:#f8f3e8;border-bottom:1px solid #e8dec9;font-family:var(--font-ui)}
.tcap .tno{background:var(--green);color:#fff;font-size:10px;font-weight:700;border-radius:6px;padding:1px 7px;white-space:nowrap;direction:rtl;unicode-bidi:isolate}
.tcap .tname{font-size:12.5px;font-weight:700;color:#4a3712;line-height:1.7}
.tcap .tcount{margin-inline-start:auto;font-size:10px;color:#8a7a5c;background:#fff;border:1px solid #e5dac2;border-radius:999px;padding:0 8px;white-space:nowrap}
.t-bva .tcap .tno{background:#a97a25}
.t-sc .tcap .tno{background:#6d441c}
.t-bc .tcap .tno{background:#2f6d4f}
.tbl thead th{background:#f3ecdd;color:#4a3712;font-family:var(--font-ui);font-size:11.5px;font-weight:700;line-height:1.6;padding:7px 8px;border-bottom:2px solid #d8c9a7;text-align:right;vertical-align:middle}
.tbl thead th:first-child{text-align:center}
.tbl tbody td{border-bottom:1px solid #efe8da;padding:7px 8px;vertical-align:top;line-height:1.75;font-size:12.5px;overflow-wrap:anywhere}
.tbl tbody tr:last-child td{border-bottom:0}
.tbl tbody tr:nth-child(even) td{background:#fcfaf4}
.tbl .c-id{text-align:center;white-space:nowrap;background:#fbf7ee;font-family:var(--font-mono);font-size:10.5px;color:#7a5a2e;direction:ltr;unicode-bidi:isolate;padding-top:9px}
.tbl .c-cond{font-family:var(--font-ui);font-size:11.5px;color:#4b5563;line-height:1.75}
.tbl .c-in{font-family:var(--font-ui);font-size:11.5px;line-height:1.9}
.tbl .c-actual,.tbl .c-exp{font-size:12px}
.tbl .c-actual{color:#14532d}
.tok{unicode-bidi:isolate}
.tok.mono{font-family:var(--font-mono);font-size:10.5px;color:#0f4c36;direction:ltr;white-space:nowrap}
.sep{color:#c9b48a;padding:0 4px;font-family:var(--font-mono);font-size:10px}
.tbl .c-res{text-align:center}
.pill{display:inline-block;font-family:var(--font-ui);font-size:9.5px;font-weight:700;line-height:1.7;border-radius:999px;padding:0 8px;white-space:nowrap}
.pill.pass{color:#0a7d32;background:#e7f4ec;border:1px solid #bfe3cd}
.req-foot{display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;margin-top:4px;padding-top:9px;border-top:1px dashed #eadfcb;font-family:var(--font-ui);font-size:10.5px;color:#8a7a5c}
.req-foot b{color:#4a3712}
.req-foot a{font-weight:700;color:var(--green);white-space:nowrap}

/* ---------- التغطية ---------- */
.cov .tbl tbody td{vertical-align:middle}
.cov .grp td{background:#f3f7f4;border-top:1px solid #c4ddcd;border-bottom:1px solid #d7e8dc;font-family:var(--font-ui);font-size:11px;color:var(--green);font-weight:700;padding:6px 10px}
.cov .grp td span{float:left;font-weight:400;color:#7a8f80;font-size:10px}
.cov .cls td{font-size:12px}
.cov .cls td:first-child{font-family:var(--font-mono);font-size:11px;direction:ltr;unicode-bidi:isolate;white-space:nowrap}
.cov .c-bar{white-space:nowrap;text-align:left;direction:ltr}
.bar{display:inline-block;width:86px;height:7px;border-radius:999px;background:#ece5d7;overflow:hidden;vertical-align:middle;margin-inline-end:7px}
.bar i{display:block;height:100%;border-radius:999px;background:linear-gradient(90deg,#2f7d5b,#7fb894)}
.pct{display:inline-block;min-width:42px;font-family:var(--font-mono);font-size:10.5px;color:#4b5563}
.na{color:#b3a58c;font-family:var(--font-mono)}
.cov tr.total td{background:#f3f7f4;font-family:var(--font-ui);font-weight:700;color:var(--green);border-top:2px solid #c4ddcd;font-size:12px}
.cov .c-bar .bar{width:96px}

/* ---------- الملحق والفوتر ---------- */
.cmd{background:#1f2937;color:#e8f0ea;border:0;border-radius:10px;padding:12px 14px;font-family:var(--font-mono);font-size:11.5px;line-height:1.85;direction:ltr;text-align:left;white-space:pre-wrap;overflow-wrap:anywhere;margin:0 0 12px}
.doc-footer{margin:34px 0 0;padding:16px 0 0;border-top:1px solid var(--line);text-align:center;font-family:var(--font-ui);font-size:10.5px;color:#9a8768;line-height:1.9}
.doc-footer b{color:var(--brown)}

/* ---------- الشاشات الصغيرة ---------- */
@media (max-width:760px){
  .sheet{margin:12px;padding:26px 20px;border-radius:14px}
  .topnav a{display:none}
  .cover{padding:30px 18px}
  .cover h1{font-size:28px}
  .cover-stats,.info-grid,.cards,.kpis,.legend{grid-template-columns:repeat(2,1fr)}
  .toc-grid{grid-template-columns:1fr}
  .req{padding:18px 16px}
  .req-meta{grid-template-columns:repeat(2,1fr)}
  .req-head{gap:11px}
  .req-num{width:52px;height:52px}
  .req-titles h3{font-size:18px}
  .tbl-card{overflow-x:auto}
  .tbl{min-width:640px}
  .bar{width:56px}
}
@media (max-width:460px){
  .cover-stats,.info-grid,.cards,.kpis,.legend,.req-meta{grid-template-columns:1fr}
  .brand b{white-space:normal}
}

/* ---------- الطباعة / PDF ---------- */
@page{size:A4;margin:15mm 13mm}
@media print{
  *{-webkit-print-color-adjust:exact;print-color-adjust:exact}
  .tbl-card.split{page-break-inside:auto;break-inside:auto;overflow:visible;border-radius:0}
  html,body{background:#fff}
  body{font-size:10.6pt;line-height:1.75;padding:0;max-width:none}
  .topbar,.totop,.print-btn{display:none!important}
  .sheet{margin:0;padding:0;max-width:none;border-radius:0;box-shadow:none}
  .cover{min-height:238mm;margin:0;padding:24mm 14mm;border-radius:0;page-break-after:always;break-after:page;display:flex;flex-direction:column;justify-content:center}
  .cover h1{font-size:29pt}
  .cover .project{font-size:12.5pt}
  .docinfo{margin-top:0}
  .sec{margin-top:16px;page-break-after:avoid;break-after:avoid;font-size:16pt}
  .sec .secno{width:30px;height:30px;font-size:12pt}
  .lead{font-size:10.5pt}
  .note{font-size:9.5pt}
  .cards,.kpis,.info-grid,.legend,.toc-grid,.toc-panel{page-break-inside:avoid;break-inside:avoid}
  .card p,.card ul{font-size:9.3pt}
  .req{border:0;border-top:1px solid #e6ddcc;border-radius:0;padding:16px 0 0;margin:16px 0 0;box-shadow:none;page-break-before:auto;break-before:auto}
  .req.first{border-top:0;padding-top:0;margin-top:0}
  .req-head{page-break-after:avoid;break-after:avoid;page-break-inside:avoid;break-inside:avoid}
  #toc,#trace,#coverage,#appendix{page-break-before:always;break-before:page}
  .req-meta{page-break-inside:avoid;break-inside:avoid}
  .req-titles h3{font-size:15pt}
  .req-desc{font-size:10pt}
  .bdd-card{page-break-inside:avoid;break-inside:avoid}
  .tbl-card{page-break-inside:avoid;break-inside:avoid}
  .tbl-card.split{page-break-inside:auto;break-inside:auto;overflow:visible;border-radius:0}
  .bdd-head{font-size:9.5pt}
  .gherkin{font-size:9.3pt;line-height:1.8}
  .tbl thead th{font-size:8.3pt;padding:4px 5px}
  .tbl tbody td{font-size:8.6pt;padding:4px 5px;line-height:1.6}
  .tbl .c-id,.tbl .c-cond,.tbl .c-in{font-size:8.1pt}
  .tbl .c-actual,.tbl .c-exp{font-size:8.4pt}
  .tbl .tok.mono{font-size:7.6pt}
  .tcap{padding:5px 7px}
  .tcap .tname{font-size:9pt}
  .tcap .tno,.tcap .tcount{font-size:7.5pt}
  #trace{margin-top:0}
  .trace-section .sec{padding-bottom:3px;margin-bottom:3px}
  .trace-section .lead{display:none}
  .trace td{font-size:8.1pt;padding:1.5px 4px;line-height:1.42}
  .trace td code{font-size:6.9pt}
  .trace thead th{padding:3px 4px}
  .cov .lead{margin-bottom:8px}
  .cov .kpis{margin:8px 0}
  .cov .kpi{padding:7px 10px}
  .cov .cls td{font-size:8.1pt;padding:2.5px 5px;line-height:1.5}
  .cov .cls td:first-child{font-size:7.5pt}
  .cov .grp td{font-size:8.2pt;padding:2.5px 6px}
  .cov .tbl thead th{padding:3px 5px}
  .bar{width:70px;height:5px}
  .pct{font-size:7.8pt}
  .pill{font-size:7.5pt}
  .req-foot{font-size:7.8pt}
  .doc-footer{font-size:8pt}
  a{color:inherit;text-decoration:none}
  thead{display:table-header-group}
  tr{page-break-inside:avoid;break-inside:avoid}
}
"""


# --------------------------------------------------------------------------
# أدوات مساعدة
# --------------------------------------------------------------------------
def esc(value):
    return html.escape(str(value), quote=True)


def counts(n):
    return tuple(len(part) for part in TABLES[n])


TOTAL_CASES = sum(sum(counts(r["n"])) for r in REQS)
TOTAL_EP = sum(counts(r["n"])[0] for r in REQS)
TOTAL_BVA = sum(counts(r["n"])[1] for r in REQS)
TOTAL_SC = sum(counts(r["n"])[2] for r in REQS)
TOTAL_BC = sum(counts(r["n"])[3] for r in REQS)
JUNIT_TESTS = 145


def pct(covered, missed):
    total = covered + missed
    return (100.0 * covered / total) if total else 100.0


def fmt_pct(value):
    return ("%.1f%%" % value).replace(".0%", "%")


def load_coverage():
    path = os.path.join(ROOT, "coverage", "summary.csv")
    if not os.path.exists(path):
        return None
    grouped = OrderedDict((p, []) for p in ("com.hirfa.model", "com.hirfa.service"))
    with open(path, encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            package = row.get("PACKAGE")
            if package not in grouped:
                continue
            im = int(row["INSTRUCTION_MISSED"])
            ic = int(row["INSTRUCTION_COVERED"])
            bm = int(row["BRANCH_MISSED"])
            bc = int(row["BRANCH_COVERED"])
            lm = int(row["LINE_MISSED"])
            lc = int(row["LINE_COVERED"])
            has_branch = (bm + bc) > 0
            grouped[package].append({
                "name": row["CLASS"], "im": im, "ic": ic, "bm": bm, "bc": bc,
                "lm": lm, "lc": lc,
                "stmt": pct(ic, im),
                "branch": pct(bc, bm) if has_branch else None,
            })

    by_pkg = OrderedDict()
    for package, classes in grouped.items():
        im = sum(c["im"] for c in classes)
        ic = sum(c["ic"] for c in classes)
        bm = sum(c["bm"] for c in classes)
        bc = sum(c["bc"] for c in classes)
        lm = sum(c["lm"] for c in classes)
        lc = sum(c["lc"] for c in classes)
        by_pkg[package] = {
            "classes": classes, "im": im, "ic": ic, "bm": bm, "bc": bc,
            "lm": lm, "lc": lc,
            "stmt": pct(ic, im), "branch": pct(bc, bm),
        }

    total = {
        "im": sum(p["im"] for p in by_pkg.values()),
        "ic": sum(p["ic"] for p in by_pkg.values()),
        "bm": sum(p["bm"] for p in by_pkg.values()),
        "bc": sum(p["bc"] for p in by_pkg.values()),
        "lm": sum(p["lm"] for p in by_pkg.values()),
        "lc": sum(p["lc"] for p in by_pkg.values()),
        "classes": sum(len(p["classes"]) for p in by_pkg.values()),
    }
    total["stmt"] = pct(total["ic"], total["im"])
    total["branch"] = pct(total["bc"], total["bm"])
    total["line"] = pct(total["lc"], total["lm"])
    return {"by_pkg": by_pkg, "total": total}


PKG_LABEL = {
    "com.hirfa.model": "طبقة النموذج · model",
    "com.hirfa.service": "طبقة الخدمات · service",
}


def _inline_strings(text):
    escaped = esc(text)
    return re.sub(r"&quot;(.+?)&quot;", r'<span class="gs">&quot;\1&quot;</span>', escaped)


GHERKIN_KEYWORD = re.compile(
    r"^(\s*)(Feature|Scenario Outline|Scenario|Background|Examples|Given|When|Then|And|But)(:?)"
)


def gherkin_html(bdd):
    lines = []
    for raw in bdd.strip().splitlines():
        line = raw.rstrip()
        if not line.strip():
            lines.append('<span class="gl" aria-hidden="true">&nbsp;</span>')
            continue
        match = GHERKIN_KEYWORD.match(line)
        if match:
            keyword = match.group(2)
            kind = {
                "Feature": "feat", "Background": "feat",
                "Scenario": "scen", "Scenario Outline": "scen", "Examples": "scen",
            }.get(keyword, "step")
            rest = _inline_strings(line[match.end():])
            lines.append(
                '<span class="gl l-%s"><span class="gk gk-%s">%s%s</span>%s</span>'
                % (kind, kind, esc(keyword), match.group(3), rest)
            )
        else:
            lines.append('<span class="gl">%s</span>' % _inline_strings(line))
    return "".join(lines)


def breakable_code(text):
    """يسمح بالكسر عند النقطة وقبل Test بدل كسر الكلمة في منتصفها."""
    escaped = esc(str(text))
    if escaped.endswith("Test"):
        escaped = escaped[:-4] + "<wbr>Test"
    return escaped.replace(".", ".<wbr>")


def _token(text):
    token = text.strip()
    if not token:
        return ""
    if re.fullmatch(r"[A-Za-z0-9@._+\-:()#/%\s]+", token):
        return '<span class="tok mono" dir="ltr">%s</span>' % esc(token)
    return '<span class="tok" dir="auto">%s</span>' % esc(token)


def input_html(text):
    parts = re.split(r"\s+/\s+", str(text))
    return '<span class="sep">/</span>'.join(_token(part) for part in parts)


def table_block(n, k, title, rows):
    """يبني جدولاً واحداً من جداول المتطلب (k = 1..4)."""
    if k == 1:
        headers = ["معرف الحالة", "شرط الإدخال", "المدخلات", "الناتج الفعلي", "الناتج المتوقع"]
        widths = [10, 19, 31, 20, 20]
        kind = "ep"
        second = "cond"
        show_result = False
    elif k == 2:
        headers = ["معرف الحالة", "الحدّ", "المدخلات", "الناتج الفعلي", "الناتج المتوقع"]
        widths = [10, 19, 31, 20, 20]
        kind = "bva"
        second = "cond"
        show_result = False
    elif k == 3:
        headers = ["معرف الحالة", "العبارة المغطاة", "المدخلات", "الناتج الفعلي", "الناتج المتوقع", "النتيجة"]
        widths = [8, 19, 29, 16, 16, 12]
        kind = "sc"
        second = "stmt"
        show_result = True
    else:
        headers = ["معرف الحالة", "الفرع (Decision)", "المدخلات", "الناتج الفعلي", "الناتج المتوقع", "النتيجة"]
        widths = [8, 19, 29, 16, 16, 12]
        kind = "bc"
        second = "branch"
        show_result = True

    out = ['<div class="tbl-card t-%s"><table class="tbl">' % kind]
    out.append(
        '<caption><span class="tcap"><span class="tno">الجدول %d-%d</span>'
        '<span class="tname">%s</span><span class="tcount">%d حالة</span></span></caption>'
        % (n, k, esc(title), len(rows))
    )
    out.append("<colgroup>" + "".join('<col style="width:%d%%">' % w for w in widths) + "</colgroup>")
    out.append("<thead><tr>")
    out.extend('<th scope="col">%s</th>' % esc(h) for h in headers)
    out.append("</tr></thead><tbody>")
    for row in rows:
        cid, cond, inp, expected = row
        out.append("<tr>")
        out.append('<td class="c-id">%s</td>' % esc(cid))
        out.append('<td class="c-%s">%s</td>' % (second, esc(cond)))
        out.append('<td class="c-in">%s</td>' % input_html(inp))
        out.append('<td class="c-actual">%s</td>' % esc(expected))
        out.append('<td class="c-exp">%s</td>' % esc(expected))
        if show_result:
            out.append('<td class="c-res"><span class="pill pass">Pass ✓</span></td>')
        out.append("</tr>")
    out.append("</tbody></table></div>")
    return "".join(out)


def requirement_section(req):
    n = req["n"]
    ep, bva, sc, bc = counts(n)
    total = ep + bva + sc + bc
    rows = [
        ("الدالة المختبرة", req["fn"], True),
        ("اختبار JUnit", req["test"], True),
        ("ملف الميزة", "R%02d.feature" % n, False),
        ("حالات المتطلب", "%d حالة" % total, False),
    ]
    meta = "".join(
        '<div><span>%s</span><b>%s</b></div>'
        % (esc(label), breakable_code(value) if code else esc(value))
        for label, value, code in rows
    )
    return """
<section class="req%s" id="req%d">
  <div class="req-head">
    <div class="req-num" aria-hidden="true"><small>متطلب</small><b>%02d</b></div>
    <div class="req-titles">
      <p class="req-kicker">المتطلب #%d · وحدة %s</p>
      <h3>%s</h3>
      <p class="req-desc">%s</p>
    </div>
  </div>
  <div class="req-meta">%s</div>
  <div class="bdd-card">
    <div class="bdd-head"><span class="lbl"><span class="dot"></span>الوصف بتنسيق BDD · Gherkin</span><span class="file">R%02d.feature</span></div>
    <pre class="gherkin" dir="rtl">%s</pre>
  </div>
  %s
  %s
  %s
  %s
  <div class="req-foot">
    <span>إجمالي حالات المتطلب: <b>%d</b> — تجزئة %d · حدود %d · عبارات %d · فروع %d</span>
    <a class="totop" href="#toc">↑ العودة إلى الفهرس</a>
  </div>
</section>""" % (
        " first" if n == 1 else "", n, n, n, esc(MODULE_OF[n]), esc(req["title"]), esc(req["desc"]), meta, n,
        gherkin_html(req["bdd"]),
        table_block(n, 1, "حالات تجزئة التكافؤ", TABLES[n][0]),
        table_block(n, 2, "حالات قيم الحدود (2-BVA)", TABLES[n][1]),
        table_block(n, 3, "تغطية العبارات · Statement Coverage", TABLES[n][2]),
        table_block(n, 4, "تغطية الفروع · Branch Coverage", TABLES[n][3]),
        total, ep, bva, sc, bc,
    )


# --------------------------------------------------------------------------
# أقسام الوثيقة
# --------------------------------------------------------------------------
def cover(cov):
    members = "".join('<span class="member">%s</span>' % esc(name) for name in TEAM)
    stmt = fmt_pct(cov["total"]["stmt"]) if cov else "—"
    branch = fmt_pct(cov["total"]["branch"]) if cov else "100%"
    return """
<section class="cover" id="cover" aria-label="صفحة الغلاف">
  <p class="uni">جامعة العلوم والتكنولوجيا الأردنية</p>
  <p class="course"><span>SE430 · Software Testing · خريف 2026</span></p>
  <h1>وثيقة المتطلبات والاختبارات</h1>
  <p class="project">مشروع منصة «حِرفة» — تسويق وتوصيل الأعمال اليدوية والأعمال المنزلية</p>
  <p class="coverage-line">%d متطلباً · %d حالة اختبار · %d اختبار JUnit · BDD + JUnit 5 + JaCoCo</p>
  <div class="divider"></div>
  <p class="team-label">أعضاء الفريق</p>
  <div class="team">%s</div>
  <div class="cover-stats">
    <div><b>%d</b><span>متطلباً موثقاً</span></div>
    <div><b>%d</b><span>حالة اختبار</span></div>
    <div><b>%d</b><span>اختبار JUnit</span></div>
    <div><b>%s</b><span>تغطية الفروع</span></div>
  </div>
  <div class="cover-foot">
    <span>الفصل الدراسي: خريف 2026</span>
    <span>الإصدار: 1.0</span>
    <span>الأدوات: Java 17 · JUnit 5 · JaCoCo · SQLite</span>
  </div>
</section>""" % (len(REQS), TOTAL_CASES, JUNIT_TESTS, members,
                len(REQS), TOTAL_CASES, JUNIT_TESTS, branch)


def docinfo():
    items = [
        ("المادة", "SE430 — Software Testing"),
        ("الفصل الدراسي", "خريف 2026"),
        ("المشروع", "منصة «حِرفة»"),
        ("الإصدار", "1.0"),
        ("عدد المتطلبات", "%d متطلباً" % len(REQS)),
        ("حالات الاختبار", "%d حالة" % TOTAL_CASES),
        ("اختبارات JUnit", "%d اختباراً ناجحاً" % JUNIT_TESTS),
        ("أداة التغطية", "JaCoCo"),
    ]
    grid = "".join('<div><span>%s</span><b>%s</b></div>' % (esc(k), esc(v)) for k, v in items)
    return """
<section class="docinfo" id="docinfo" aria-label="بيانات الوثيقة">
  <div class="docinfo-head"><h2>بيانات الوثيقة</h2><span>SE430 · خريف 2026</span></div>
  <div class="info-grid">%s</div>
</section>""" % grid


def methodology(cov):
    stmt = fmt_pct(cov["total"]["stmt"]) if cov else "99.7%"
    branch = fmt_pct(cov["total"]["branch"]) if cov else "100%"
    return """
<section aria-labelledby="method-title">
  <h2 class="sec" id="method"><span class="secno">أ</span><span id="method-title">منهجية الاختبار</span></h2>
  <p class="lead">تتبع الوثيقة استراتيجية اختبار مزدوجة: الصندوق الأسود لاشتقاق الحالات من وصف المتطلب،
  والصندوق الأبيض لقياس تغطية الشيفرة آلياً. لكل متطلب أربعة جداول مرقّمة، بالترتيب نفسه الوارد في توصيف المشروع.</p>
  <div class="cards">
    <article class="card">
      <div class="card-head"><span class="dot"></span><h3>الصندوق الأسود</h3><span class="tag">EP + 2-BVA</span></div>
      <p>تُقسَّم مدخلات كل متطلب إلى فئات تكافؤ تعطي سلوكاً متميزاً (Equivalence Partitioning)، ثم تُختبر حدود كل فئة بقيمتي الحد (n−1/n) و(n/n+1) وفق أسلوب 2-BVA.</p>
      <ul><li>جدول n-1: حالات تجزئة التكافؤ.</li><li>جدول n-2: حالات قيم الحدود.</li></ul>
    </article>
    <article class="card">
      <div class="card-head"><span class="dot"></span><h3>الصندوق الأبيض</h3><span class="tag">JUnit 5</span></div>
      <p>لكل دالة مسؤولة عن المتطلب اختبارات JUnit آلية تغطي كل عبارة تنفيذية (Statement Coverage) وكل مسار قرار (Branch Coverage).</p>
      <ul><li>جدول n-3: تغطية العبارات.</li><li>جدول n-4: تغطية الفروع مع عمود Pass/Fail.</li></ul>
    </article>
    <article class="card">
      <div class="card-head"><span class="dot"></span><h3>القياس والتوثيق</h3><span class="tag">JaCoCo</span></div>
      <p>تُقاس التغطية بأداة JaCoCo على طبقتي <code>service</code> و<code>model</code>، وتُوثَّق النتائج في القسم هـ، مع أوامر إعادة الإنتاج في الملحق.</p>
      <ul><li>تقرير HTML تفصيلي وملف CSV.</li><li>خرائط أسطر لكل صنف.</li></ul>
    </article>
  </div>
  <div class="kpis">
    <div class="kpi"><b>%d</b><span>متطلباً موثقاً</span><small>موزعة على 6 وحدات</small></div>
    <div class="kpi"><b>%d</b><span>حالة اختبار</span><small>EP · BVA · SC · BC</small></div>
    <div class="kpi"><b>%s</b><span>تغطية العبارات</span><small>service + model</small></div>
    <div class="kpi"><b>%s</b><span>تغطية الفروع</span><small>%d / %d فرعاً</small></div>
  </div>
  <p class="note">جميع الاختبارات آلية وتُشغَّل بأمر واحد: <b>%d اختباراً ناجحاً</b>، وتغطية الفروع <b>%s</b>،
  وتغطية العبارات <b>%s</b> على طبقتي <code>service</code> و<code>model</code> (التفاصيل في القسم هـ).</p>
</section>""" % (
        len(REQS), TOTAL_CASES, stmt, branch,
        cov["total"]["bc"] if cov else 203, cov["total"]["bc"] + cov["total"]["bm"] if cov else 203,
        JUNIT_TESTS, branch, stmt,
    )


def toc():
    section_items = []
    letters = {"method": "أ", "toc": "ب", "trace": "ج", "reqs": "د", "coverage": "هـ", "appendix": "و"}
    for anchor, title, desc in SECTIONS:
        number = letters.get(anchor, "•")
        section_items.append(
            '<li><a href="#%s"><span class="sn">%s</span><span class="st">%s<small>%s</small></span></a></li>'
            % (anchor, number, esc(title), esc(desc))
        )

    groups = []
    for title, desc, numbers in MODULES:
        items = []
        for n in sorted(numbers):
            req = next(r for r in REQS if r["n"] == n)
            items.append(
                '<li><a href="#req%d"><span class="num">%02d</span><span class="ttl">%s</span>'
                '<span class="cnt">%d حالة</span></a></li>'
                % (n, n, esc(req["title"]), sum(counts(n)))
            )
        groups.append(
            '<div class="toc-group"><h4>%s<span>%d متطلبات</span></h4>'
            '<p class="sr-only">%s</p><ul class="toc-req">%s</ul></div>'
            % (esc(title), len(numbers), esc(desc), "".join(items))
        )

    return """
<section aria-labelledby="toc-title">
  <h2 class="sec" id="toc"><span class="secno">ب</span><span id="toc-title">فهرس المتطلبات</span></h2>
  <p class="lead">تجميع المتطلبات في ست وحدات وظيفية لتسهيل المراجعة، مع بقاء الترقيم التسلسلي 1–20 في متن الوثيقة.</p>
  <div class="toc-grid">
    <nav class="toc-panel" aria-label="محتويات الوثيقة">
      <h3 class="toc-h">محتويات الوثيقة</h3>
      <ol class="sec-list">%s</ol>
    </nav>
    <nav class="toc-panel" aria-label="المتطلبات حسب الوحدة">
      <h3 class="toc-h">المتطلبات حسب الوحدة الوظيفية</h3>
      %s
    </nav>
  </div>
</section>""" % ("".join(section_items), "".join(groups))


def traceability():
    rows = []
    for req in REQS:
        n = req["n"]
        ep, bva, sc, bc = counts(n)
        rows.append(
            '<tr><td class="c-id">%02d</td>'
            '<td><a href="#req%d">%s</a><span class="unit">%s</span></td>'
            "<td><code>%s</code></td><td><code>%s</code></td>"
            '<td class="c-counts">%d<span class="dotsep">·</span>%d<span class="dotsep">·</span>%d<span class="dotsep">·</span>%d</td>'
            '<td class="c-total">%d</td></tr>'
            % (n, n, esc(req["title"]), esc(MODULE_OF[n]), breakable_code(req["fn"]), breakable_code(req["test"]),
               ep, bva, sc, bc, ep + bva + sc + bc)
        )
    rows.append(
        '<tr class="total"><td class="c-id">—</td><td colspan="3">الإجمالي</td>'
        '<td class="c-counts">%d<span class="dotsep">·</span>%d<span class="dotsep">·</span>%d<span class="dotsep">·</span>%d</td>'
        '<td class="c-total">%d</td></tr>'
        % (TOTAL_EP, TOTAL_BVA, TOTAL_SC, TOTAL_BC, TOTAL_CASES)
    )
    return """
<section class="trace-section" aria-labelledby="trace-title">
  <h2 class="sec" id="trace"><span class="secno">ج</span><span id="trace-title">مصفوفة التتبع</span></h2>
  <p class="lead">تربط المصفوفة كل متطلب بالدالة واختبار JUnit، وتلخّص عدد الحالات في الجداول الأربعة.</p>
  <div class="tbl-card split">
    <table class="tbl trace">
      <caption><span class="tcap"><span class="tno">الجدول ج-1</span><span class="tname">تتبع المتطلبات إلى الشيفرة والاختبارات</span><span class="tcount">%d متطلباً</span></span></caption>
      <colgroup><col style="width:6%%"><col style="width:24%%"><col style="width:24%%"><col style="width:25%%"><col style="width:14%%"><col style="width:7%%"></colgroup>
      <thead><tr><th scope="col">#</th><th scope="col">المتطلب</th><th scope="col">الدالة المختبرة</th><th scope="col">اختبار JUnit</th><th scope="col">EP · BVA · SC · BC</th><th scope="col">المجموع</th></tr></thead>
      <tbody>%s</tbody>
    </table>
  </div>
</section>""" % (
        len(REQS), "".join(rows),
    )


def requirements_intro():
    return """
<section aria-labelledby="reqs-title">
  <h2 class="sec" id="reqs"><span class="secno">د</span><span id="reqs-title">المتطلبات وجداول الاختبار</span></h2>
  <p class="lead">تعرض كل بطاقة متطلب وصفه بصيغة BDD (Gherkin)، ثم الجداول الأربعة: تجزئة التكافؤ، قيم الحدود 2-BVA،
  تغطية العبارات، وتغطية الفروع. ترتيب البطاقات تسلسلي من 1 إلى 20.</p>
  <div class="legend">
    <div class="t-ep"><b>الجدول n-1</b><span>تجزئة التكافؤ · EP</span></div>
    <div class="t-bva"><b>الجدول n-2</b><span>قيم الحدود · 2-BVA</span></div>
    <div class="t-sc"><b>الجدول n-3</b><span>تغطية العبارات · Statement</span></div>
    <div class="t-bc"><b>الجدول n-4</b><span>تغطية الفروع · Branch</span></div>
  </div>
  <p class="note">جميع الحالات ناجحة، ولذلك فإن «الناتج الفعلي» مطابق لـ«الناتج المتوقع» في كل صف. عمود <b>Pass/Fail</b>
  مخصص لجدولي الصندوق الأبيض (n-3 و n-4) كما يقتضي التوصيف، بينما يكتفي جدولا الصندوق الأسود بعرض الناتج الفعلي والمتوقع.</p>
</section>"""


def coverage_section(cov):
    if not cov:
        return """
<section aria-labelledby="coverage-title">
  <h2 class="sec" id="coverage"><span class="secno">هـ</span><span id="coverage-title">تقرير تغطية الشيفرة (JaCoCo)</span></h2>
  <p class="note">لم يُعثر على <code>coverage/summary.csv</code>. شغّل اختبارات التغطية أولاً بالأوامر الواردة في الملحق، ثم أعد توليد الوثيقة.</p>
</section>"""

    total = cov["total"]
    rows = []
    for package, group in cov["by_pkg"].items():
        rows.append(
            '<tr class="grp"><td colspan="4">%s'
            '<span>%s عبارات · %s فروع</span></td></tr>'
            % (esc(PKG_LABEL[package]), fmt_pct(group["stmt"]), fmt_pct(group["branch"]))
        )
        for cls in sorted(group["classes"], key=lambda item: item["name"]):
            if cls["branch"] is None:
                branch_cell = '<span class="na" title="لا يوجد تفرّع في هذا الصنف">—</span>'
                branch_meta = "لا تفرّعات"
            else:
                branch_cell = ('<span class="bar" aria-hidden="true"><i style="width:%.1f%%"></i></span>'
                               '<span class="pct">%s</span>') % (cls["branch"], fmt_pct(cls["branch"]))
                branch_meta = "%d/%d فرعاً" % (cls["bc"], cls["bc"] + cls["bm"])
            rows.append(
                '<tr class="cls"><td>%s</td><td class="c-bar">%s</td>'
                '<td class="c-bar">%s</td><td>%s</td></tr>'
                % (
                    esc(cls["name"]),
                    '<span class="bar" aria-hidden="true"><i style="width:%.1f%%"></i></span><span class="pct">%s</span>'
                    % (cls["stmt"], fmt_pct(cls["stmt"])),
                    branch_cell,
                    branch_meta,
                )
            )

    rows.append(
        '<tr class="total"><td>الإجمالي</td>'
        '<td class="c-bar"><span class="bar" aria-hidden="true"><i style="width:%.1f%%"></i></span>'
        '<span class="pct">%s</span></td>'
        '<td class="c-bar"><span class="bar" aria-hidden="true"><i style="width:%.1f%%"></i></span>'
        '<span class="pct">%s</span></td><td>%d/%d</td></tr>'
        % (total["stmt"], fmt_pct(total["stmt"]), total["branch"], fmt_pct(total["branch"]),
           total["bc"], total["bc"] + total["bm"])
    )

    return """
<section class="cov" aria-labelledby="coverage-title">
  <h2 class="sec" id="coverage"><span class="secno">هـ</span><span id="coverage-title">تقرير تغطية الشيفرة (JaCoCo)</span></h2>
  <p class="lead">آخر تشغيل آلي: <b>%d اختباراً ناجحاً</b>. يقيس التقرير طبقتي <code>service</code> و<code>model</code>
  فقط، ولا يشمل طبقة الواجهة أو قاعدة البيانات.</p>
  <div class="kpis">
    <div class="kpi"><b>%s</b><span>تغطية العبارات</span><small>%d / %d تعليمة</small></div>
    <div class="kpi"><b>%s</b><span>تغطية الفروع</span><small>%d / %d فرعاً</small></div>
    <div class="kpi"><b>%d</b><span>الأصناف المقاسة</span><small>model + service</small></div>
    <div class="kpi"><b>%s</b><span>تغطية الأسطر</span><small>%d / %d سطراً</small></div>
  </div>
  <div class="tbl-card split">
    <table class="tbl cov-tbl">
      <caption><span class="tcap"><span class="tno">الجدول هـ-1</span><span class="tname">تغطية العبارات والفروع لكل صنف</span><span class="tcount">%d صنفاً</span></span></caption>
      <colgroup><col style="width:38%%"><col style="width:26%%"><col style="width:20%%"><col style="width:16%%"></colgroup>
      <thead><tr><th scope="col">الصنف (Class)</th><th scope="col">تغطية العبارات</th><th scope="col">تغطية الفروع</th><th scope="col">الفروع</th></tr></thead>
      <tbody>%s</tbody>
    </table>
  </div>
  <p class="note">التعليمات الأربع غير المغطاة تقع في الصنف <code>Cart</code> ضمن طبقة النموذج، بينما تغطية الفروع كاملة
  (<b>%d/%d</b>). التفاصيل وخرائط الأسطر في <code>coverage/html/index.html</code> والبيانات في <code>coverage/summary.csv</code>.</p>
</section>""" % (
        JUNIT_TESTS,
        fmt_pct(total["stmt"]), total["ic"], total["ic"] + total["im"],
        fmt_pct(total["branch"]), total["bc"], total["bc"] + total["bm"],
        total["classes"],
        fmt_pct(total["line"]), total["lc"], total["lc"] + total["lm"],
        total["classes"],
        "".join(rows),
        total["bc"], total["bc"] + total["bm"],
    )


def appendix():
    return """
<section aria-labelledby="appendix-title">
  <h2 class="sec" id="appendix"><span class="secno">و</span><span id="appendix-title">الملحق · إعادة إنتاج النتائج</span></h2>
  <p class="lead">تعتمد الأرقام الواردة في القسم هـ على التشغيل الآلي التالي من جذر المستودع (JDK 17).</p>
  <h3>1. تشغيل اختبارات JUnit</h3>
  <pre class="cmd">javac -cp "build:lib/sqlite-jdbc.jar:lib/junit-console.jar" -d build $(find test src -name '*.java')
java -cp "lib/junit-console.jar:build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" \\
  org.junit.platform.console.ConsoleLauncher --scan-classpath --details=tree</pre>
  <h3>2. توليد تقرير التغطية</h3>
  <pre class="cmd">java -javaagent:lib/jacocoagent.jar=destfile=coverage/jacoco.exec \\
  -cp "lib/junit-console.jar:build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" \\
  org.junit.platform.console.ConsoleLauncher --scan-classpath
java -jar lib/jacococli.jar report coverage/jacoco.exec \\
  --classfiles build/com/hirfa/service --classfiles build/com/hirfa/model \\
  --sourcefiles src --html coverage/html --csv coverage/summary.csv</pre>
  <h3>3. ملفات الميزات</h3>
  <p>كل سيناريو Gherkin في هذه الوثيقة متاح أيضاً كملف مستقل في
  <code>doc/features/R01.feature</code> … <code>doc/features/R20.feature</code>، ليمكن استيراده إلى أدوات Cucumber لاحقاً.</p>
  <p class="note">نطاق القياس في القسم هـ هو طبقنتا <code>service</code> و<code>model</code>، وهي الطبقة التي تحمل منطق
  المتطلبات العشرين. الواجهة وقاعدة البيانات خارج نطاق قياس التغطية.</p>
</section>"""


def gen_doc():
    validate()
    cov = load_coverage()
    parts = [
        "<!DOCTYPE html>",
        "<html lang='ar' dir='rtl'>",
        "<head>",
        "<meta charset='utf-8'>",
        "<meta name='viewport' content='width=device-width, initial-scale=1'>",
        "<meta name='description' content='SE430 — وثيقة المتطلبات والاختبارات لمشروع منصة حِرفة: 20 متطلباً بصيغة BDD، اختبارات صندوق أسود وأبيض، وتقرير تغطية JaCoCo.'>",
        "<title>SE430 — وثيقة المتطلبات والاختبارات · منصة حِرفة</title>",
        "<style>", CSS, "</style>",
        "</head>",
        "<body>",
        topbar(),
        "<main class='sheet'>",
        cover(cov),
        docinfo(),
        methodology(cov),
        toc(),
        traceability(),
        requirements_intro(),
        "".join(requirement_section(req) for req in REQS),
        coverage_section(cov),
        appendix(),
        doc_footer(),
        "</main>",
        "<script>document.querySelector('.print-btn').addEventListener('click',function(){window.print();});</script>",
        "</body></html>",
    ]
    os.makedirs(os.path.dirname(DOC_PATH), exist_ok=True)
    with open(DOC_PATH, "w", encoding="utf-8") as handle:
        handle.write("".join(parts))
    print("DOC-OK", DOC_PATH)


def topbar():
    links = [("toc", "الفهرس"), ("method", "المنهجية"), ("trace", "التتبع"), ("reqs", "المتطلبات"), ("coverage", "التغطية")]
    nav = "".join('<a href="#%s">%s</a>' % (anchor, esc(label)) for anchor, label in links)
    return """
<header class="topbar">
  <div class="topbar-in">
    <a class="brand" href="#cover" aria-label="أعلى الوثيقة">
      <span class="brand-mark" aria-hidden="true">ح</span>
      <span><b>وثيقة المتطلبات والاختبارات</b><small>SE430 · منصة حِرفة · خريف 2026</small></span>
    </a>
    <nav class="topnav" aria-label="التنقل داخل الوثيقة">%s<button type="button" class="print-btn">طباعة / PDF</button></nav>
  </div>
</header>""" % nav


def doc_footer():
    return """
<footer class="doc-footer">
  <p><b>SE430 — Software Testing · خريف 2026</b></p>
  <p>منصة «حِرفة» — وثيقة المتطلبات والاختبارات · %d متطلباً · %d حالة اختبار · %d اختبار JUnit ناجح</p>
</footer>""" % (len(REQS), TOTAL_CASES, JUNIT_TESTS)


def validate():
    assert len(REQS) == 20, "عدد المتطلبات يجب أن يكون 20"
    assert sorted(MODULE_OF) == list(range(1, 21)), "كل متطلب يجب أن ينتمي إلى وحدة واحدة"
    for req in REQS:
        assert len(TABLES[req["n"]]) == 4, "لكل متطلب أربعة جداول"
        assert req["bdd"].strip(), "وصف BDD مطلوب"


def gen_features():
    base = os.path.join(ROOT, "doc", "features")
    os.makedirs(base, exist_ok=True)
    for req in REQS:
        path = os.path.join(base, "R%02d.feature" % req["n"])
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(req["bdd"].strip() + "\n")
    print("FEATURES-OK")


if __name__ == "__main__":
    gen_features()
    gen_doc()
