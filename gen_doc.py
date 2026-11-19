import html
import os
import re

from docgen_data import REQS
from docgen_tables import CASES

ROOT = os.path.dirname(os.path.abspath(__file__))
DOC_PATH = os.path.join(ROOT, "requirements-and-tests.html")
FEATURES_PATH = os.path.join(ROOT, "features")
ARABIC = re.compile(r"[\u0600-\u06ff]")

CSS = """
:root{font-family:Arial,Helvetica,sans-serif;color:#26332d;background:#f3f5f2}
*{box-sizing:border-box}
body{margin:0;line-height:1.55}
header{background:#173f35;color:#fff;padding:22px max(calc((100vw - 1060px)/2),24px)}
header h1{margin:0;font-size:1.7rem}
header p{margin:5px 0 0;color:#d9e8dc}
nav{background:#fff;border-bottom:1px solid #dbe3dc;padding:10px max(calc((100vw - 1060px)/2),24px);display:flex;gap:18px;flex-wrap:wrap}
a{color:#17634c}
main{max-width:1060px;margin:26px auto;padding:0 24px 48px}
section{background:#fff;border:1px solid #dbe3dc;border-radius:12px;padding:22px;margin:18px 0;break-inside:avoid}
h2{color:#173f35;margin-top:0}
h3{margin:14px 0 7px;color:#2e5144}
.summary{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:12px}
.summary div{background:#f2f7f3;border-radius:8px;padding:12px}
.summary b{display:block;color:#173f35}
table{width:100%;border-collapse:collapse;margin:12px 0}
th,td{text-align:left;vertical-align:top;padding:9px;border:1px solid #dbe3dc}
th{background:#edf3ee}
pre{white-space:pre-wrap;background:#f5f7f5;border-left:3px solid #4b8c70;padding:14px;overflow-wrap:anywhere}
code{font-family:Consolas,monospace}
.muted{color:#607167}
.feature{margin-top:24px;padding-top:10px;border-top:1px solid #dbe3dc}
footer{text-align:center;color:#607167;padding:25px}
@media print{body{background:#fff}nav{display:none}main{max-width:none;padding:0 12px}section{box-shadow:none;page-break-inside:avoid}a{color:inherit;text-decoration:none}}
"""


def esc(value):
    return html.escape(str(value), quote=True)


def validate():
    if len(REQS) != 20:
        raise ValueError("The requirements document must contain exactly 20 requirements.")
    if set(CASES) != {req["n"] for req in REQS}:
        raise ValueError("Every requirement must have black-box test cases.")
    for req in REQS:
        if not req["bdd"].strip() or len(CASES[req["n"]]) < 2:
            raise ValueError(f"Requirement R{req['n']:02} is missing test content.")
        values = [req["title"], req["desc"], req["fn"], req["test"], req["bdd"]]
        values.extend(value for row in CASES[req["n"]] for value in row)
        if any(ARABIC.search(value) for value in values):
            raise ValueError(f"Requirement R{req['n']:02} contains non-English text.")


def requirement_section(req):
    rows = "".join(
        "<tr><td>%s</td><td>%s</td><td>%s</td></tr>" % tuple(map(esc, row))
        for row in CASES[req["n"]]
    )
    return """
<section id="r%02d">
  <h2>R%02d — %s</h2>
  <p>%s</p>
  <p class="muted"><b>Implementation:</b> <code>%s</code> &nbsp; <b>JUnit:</b> <code>%s</code></p>
  <h3>BDD scenarios</h3>
  <pre>%s</pre>
  <h3>Black-box test design</h3>
  <table><thead><tr><th>Technique / partition</th><th>Input</th><th>Expected result</th></tr></thead>
  <tbody>%s</tbody></table>
</section>
""" % (req["n"], req["n"], esc(req["title"]), esc(req["desc"]), esc(req["fn"]),
       esc(req["test"]), esc(req["bdd"]), rows)


def generate_html():
    validate()
    toc = ' <span aria-hidden="true">|</span> '.join(
        '<a href="#r%02d">R%02d</a>' % (req["n"], req["n"]) for req in REQS)
    content = "".join(requirement_section(req) for req in REQS)
    html_doc = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Hirfa Requirements and Test Design — SE430</title>
<style>%s</style>
</head>
<body>
<header><h1>Hirfa — Requirements and Test Design</h1>
<p>SE430 Software Testing Project · Fall 2026</p></header>
<nav aria-label="Requirements navigation">%s</nav>
<main>
<section id="overview">
  <h2>Project overview</h2>
  <p>Hirfa is a Java and SQLite handmade-goods marketplace. Customers can discover products,
  manage a cart, place and track orders, and pay using supported methods. Artisans create
  accounts and manage their own products and stock.</p>
  <div class="summary">
    <div><b>Functional requirements</b>20</div>
    <div><b>BDD format</b>Gherkin scenarios</div>
    <div><b>Black-box methods</b>Equivalence partitioning and boundary-value analysis</div>
    <div><b>White-box method</b>JUnit 5 tests with JaCoCo coverage reporting</div>
  </div>
</section>
<section id="team">
  <h2>Team responsibilities</h2>
  <table><thead><tr><th>Member</th><th>Focus areas</th></tr></thead><tbody>
    <tr><td>Elaf Alkhatib</td><td>Artisan requirements, delivery fees, wallet and loyalty, requirements documentation</td></tr>
    <tr><td>Dana Haddad</td><td>Persistence and models, cart, payments, order lifecycle, artisan reports</td></tr>
    <tr><td>Raghed Ananbeh</td><td>Products, discounts, delivery estimates, service setup</td></tr>
    <tr><td>Alaa Alomari</td><td>Customer accounts, checkout, web interface, test and coverage documentation</td></tr>
  </tbody></table>
</section>
<section id="method">
  <h2>Testing approach</h2>
  <p>Black-box cases cover valid and invalid input partitions and relevant numeric boundaries.
  White-box tests are organized by service in <code>test/com/hirfa/</code>; run them with the
  bundled JUnit ConsoleLauncher. Generate a JaCoCo report using the commands in the repository
  README. Coverage values should be taken from the report generated for the current source revision.</p>
  <p>The report covers the model, repository, service, and web application packages.</p>
</section>
<section id="trace">
  <h2>Traceability</h2>
  <table><thead><tr><th>Requirements</th><th>Implementation</th><th>JUnit test</th></tr></thead><tbody>%s</tbody></table>
</section>
%s
<section id="run">
  <h2>Execution</h2>
  <p>Build and run the website from the repository root using JDK 17 or later:</p>
  <pre>javac -cp lib/sqlite-jdbc.jar -d build $(find src -name '*.java')
java -cp "build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" com.hirfa.web.WebServer handmade.db 8080</pre>
  <p>Run the JUnit suite with the commands in <code>README.md</code>. Standalone Gherkin files are
  generated under <code>features/R01.feature</code> through <code>R20.feature</code>.</p>
</section>
</main>
<footer>Hirfa — Handmade Delivery Platform · SE430 Software Testing Project</footer>
</body>
</html>""" % (CSS, toc, traceability_rows(), content)
    if ARABIC.search(html_doc):
        raise ValueError("Generated HTML contains non-English text.")
    os.makedirs(os.path.dirname(DOC_PATH), exist_ok=True)
    with open(DOC_PATH, "w", encoding="utf-8") as handle:
        handle.write(html_doc)
    return DOC_PATH


def traceability_rows():
    rows = []
    for req in REQS:
        rows.append("<tr><td>R%02d — %s</td><td><code>%s</code></td><td><code>%s</code></td></tr>" %
                    (req["n"], esc(req["title"]), esc(req["fn"]), esc(req["test"])))
    return "".join(rows)


def generate_features():
    os.makedirs(FEATURES_PATH, exist_ok=True)
    expected = set()
    for req in REQS:
        path = os.path.join(FEATURES_PATH, "R%02d.feature" % req["n"])
        text = req["bdd"].strip() + "\n"
        if ARABIC.search(text):
            raise ValueError(f"Generated feature R{req['n']:02} contains non-English text.")
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(text)
        expected.add(path)
    for filename in os.listdir(FEATURES_PATH):
        path = os.path.join(FEATURES_PATH, filename)
        if filename.endswith(".feature") and path not in expected:
            os.remove(path)


if __name__ == "__main__":
    generate_features()
    print("Generated", generate_html())
