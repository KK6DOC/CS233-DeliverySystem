from html import escape
from pathlib import Path
import math
import re
from urllib.parse import quote

from reportlab.graphics.shapes import Drawing, Line, Polygon, PolyLine, Rect, String
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import (
    KeepTogether, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle,
)

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / "output" / "pdf" / "CS233_Nobody_Loves_Java.pdf"
BODY = ParagraphStyle("Body", fontName="Times-Roman", fontSize=12, leading=24,
                      firstLineIndent=36, spaceAfter=0, splitLongWords=True)
HEADING = ParagraphStyle("Heading", parent=BODY, fontName="Times-Bold", firstLineIndent=0,
                         spaceBefore=12, spaceAfter=6, keepWithNext=True)
CENTER = ParagraphStyle("Center", parent=BODY, firstLineIndent=0, alignment=TA_CENTER)
TITLE = ParagraphStyle("Title", parent=CENTER, fontName="Times-Bold", fontSize=12, leading=24)
MAIN_HEADING = ParagraphStyle("MainHeading", parent=HEADING, alignment=TA_CENTER)
CAPTION = ParagraphStyle("Caption", parent=BODY, firstLineIndent=0, keepWithNext=True)
REFERENCE = ParagraphStyle("Reference", parent=BODY, firstLineIndent=-36, leftIndent=36)
LIST = ParagraphStyle("List", parent=BODY, firstLineIndent=0, leftIndent=18, bulletIndent=0)
CELL = ParagraphStyle("Cell", parent=BODY, fontSize=10, leading=14, firstLineIndent=0, spaceAfter=0)
NOTE = ParagraphStyle("Note", parent=BODY, fontSize=10, leading=14, firstLineIndent=0)


# Converts the small amount of Markdown formatting used in our design notes.
def inline(text):
    text = escape(text)
    text = re.sub(r"`([^`]+)`", r'<font name="Courier" size="9">\1</font>', text)

    def link(match):
        label, target = match.group(1), match.group(2)
        if not target.startswith("https://"):
            target = ("https://github.com/KK6DOC/CS233-DeliverySystem/blob/feature/delivery-system/docs/"
                      + quote(target))
        return f'<link href="{target}" color="#1b443a">{label}</link>'

    return re.sub(r"\[([^\]]+)\]\(([^)]+)\)", link, text)


# Draws a readable compact class box for the report figures.
def box(drawing, x, y, width, height, name, lines):
    drawing.add(Rect(x, y, width, height, fillColor=colors.white, strokeWidth=0.7))
    drawing.add(Rect(x, y + height - 23, width, 23, fillColor=colors.HexColor("#f1f3f0"), strokeWidth=0.7))
    drawing.add(String(x + 7, y + height - 15, name, fontName="Times-Bold", fontSize=10))
    for index, text in enumerate(lines):
        drawing.add(String(x + 7, y + height - 38 - index * 12, text, fontName="Courier", fontSize=8.5))


# Connects class boxes and uses a hollow triangle for inheritance.
def arrow(drawing, points, inheritance=False):
    drawing.add(PolyLine([value for point in points for value in point], strokeWidth=0.7))
    end, before = points[-1], points[-2]
    angle = math.atan2(end[1] - before[1], end[0] - before[0])
    left = (end[0] - 9 * math.cos(angle - 0.5), end[1] - 9 * math.sin(angle - 0.5))
    right = (end[0] - 9 * math.cos(angle + 0.5), end[1] - 9 * math.sin(angle + 0.5))
    drawing.add(Polygon([*end, *left, *right], strokeWidth=0.7,
                        fillColor=colors.white if inheritance else colors.black))


# Shows shared account information and the three inherited user roles.
def user_figure():
    drawing = Drawing(468, 450)
    box(drawing, 104, 285, 260, 130, "User (abstract)", [
        "- username: String", "- passwordHash: String", "- email: String",
        "+ login(password): boolean", "+ getRole(): String [abstract]", "+ viewOptions(): void [abstract]",
    ])
    box(drawing, 0, 100, 140, 95, "Admin extends User", ["+ getRole(): String", "+ viewOptions(): void"])
    box(drawing, 164, 100, 140, 95, "Customer extends User", ["+ getRole(): String", "+ viewOptions(): void"])
    box(drawing, 328, 40, 140, 200, "Driver extends User", [
        "- availability: boolean", "- ratings: Deque<Integer>", "- averageRating: double",
        "~ addRating(int)", "~ restoreRatings(Deque)", "+ compareTo(Driver): int",
        "+ getRole(): String", "+ viewOptions(): void",
    ])
    arrow(drawing, [(70, 195), (70, 255), (234, 255), (234, 285)], True)
    arrow(drawing, [(234, 195), (234, 285)], True)
    arrow(drawing, [(398, 240), (398, 255), (234, 255), (234, 285)], True)
    drawing.add(String(12, 20, "Driver also implements Comparable<Driver>.", fontName="Times-Italic", fontSize=10))
    return drawing


# Shows the food model and the item snapshots kept by each order.
def order_figure():
    drawing = Drawing(468, 430)
    box(drawing, 0, 250, 210, 140, "Menu", [
        "- menu: HashMap<String,MenuItem>", "+ addMenuItem(MenuItem)", "+ updateMenu(MenuItem)",
        "+ removeMenuItem(String)", "+ getItem(String): MenuItem", "+ getItems(): ArrayList<MenuItem>",
    ])
    box(drawing, 258, 120, 210, 270, "Order", [
        "- orderNum: int", "- customer: String", "- items: ArrayList<MenuItem>", "- status: Status",
        "- driver: String", "- rating: int", "+ calculateTotalCost(): BigDecimal",
        "+ canCancel(): boolean", "~ assignDriver(String)", "~ startDelivery()", "~ completeDelivery()", "~ cancel()", "~ rate(int)",
    ])
    box(drawing, 0, 30, 210, 130, "MenuItem (immutable)", [
        "- name: String", "- price: BigDecimal", "+ getName(): String", "+ getPrice(): BigDecimal",
    ])
    arrow(drawing, [(105, 250), (105, 160)])
    drawing.add(String(115, 200, "0..* menu items", fontName="Times-Roman", fontSize=10))
    arrow(drawing, [(258, 195), (232, 195), (232, 95), (210, 95)])
    drawing.add(String(222, 212, "1..*", fontName="Times-Roman", fontSize=10))
    drawing.add(String(0, 6, "Order stores customer and driver usernames; the system validates ownership.",
                       fontName="Times-Italic", fontSize=10))
    return drawing


# Shows how the two interfaces share the same backend and file manager.
def app_figure():
    drawing = Drawing(468, 540)
    box(drawing, 168, 455, 132, 55, "Main", ["+ main(String[])"])
    box(drawing, 0, 310, 200, 95, "DeliveryApp extends JFrame", [
        "- system: DeliverySystem", "- cart: LinkedHashMap", "- showDashboard(String)", "- activityPage(): JPanel",
    ])
    box(drawing, 268, 310, 200, 95, "TerminalApp", [
        "- system: DeliverySystem", "- input: Scanner", "+ run()", "- customerOption(String)",
    ])
    box(drawing, 128, 135, 212, 150, "DeliverySystem", [
        "- users: LinkedHashMap", "- allOrders: LinkedHashMap", "- ordersQueue: Queue<Order>",
        "- readyDrivers: PriorityQueue", "+ placeOrder(List<String>)", "+ rateDriver(int,int)", "+ getActivity()",
    ])
    box(drawing, 160, 15, 160, 85, "FileManager", [
        "+ loadMenu(), loadUsers()", "+ loadOrders(users)", "+ appendOrder(...)", "+ log(event,details)",
    ])
    box(drawing, 0, 15, 140, 80, "PasswordUtil", ["+ hash(String)", "+ matches(String,String)"])
    box(drawing, 350, 15, 118, 90, "ActivityEntry", ["- time", "- role, username", "- event, details"])
    arrow(drawing, [(168, 480), (100, 480), (100, 405)])
    arrow(drawing, [(300, 480), (368, 480), (368, 405)])
    arrow(drawing, [(234, 455), (234, 285)])
    arrow(drawing, [(100, 310), (100, 295), (180, 295), (180, 285)])
    arrow(drawing, [(368, 310), (368, 295), (288, 295), (288, 285)])
    arrow(drawing, [(234, 135), (234, 100)])
    arrow(drawing, [(160, 60), (140, 60)])
    arrow(drawing, [(320, 60), (350, 60)])
    return drawing


# Adds the APA-style page number in the upper-right corner.
def page_number(canvas, document):
    canvas.saveState()
    canvas.setFont("Times-Roman", 12)
    canvas.drawRightString(7.5 * inch, 10.5 * inch, str(document.page))
    canvas.restoreState()


# Builds the report from the same design notes linked in the README.
def build_report():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    story = [Spacer(1, 100), Paragraph("CS233 Food Delivery System", TITLE),
             Paragraph("Revised Design and Implementation", TITLE), Spacer(1, 24),
             Paragraph("Joshua Kelly, Lincoln Wornardt, Niko Cimbur, and William Ho", CENTER),
             Paragraph("Cuesta College", CENTER), Paragraph("CS 233 (75701)", CENTER),
             Paragraph("Prof. Dube", CENTER), Paragraph("November 30, 2026", CENTER),
             PageBreak()]
    lines = (ROOT / "docs" / "design.md").read_text().splitlines()
    index = 0
    table_number = 0
    while index < len(lines):
        line = lines[index].strip()
        if not line:
            index += 1
            continue
        if line.startswith("#"):
            style = HEADING if line.startswith("###") else MAIN_HEADING
            story.append(Paragraph(inline(line.lstrip("# ")), style))
            index += 1
        elif line.startswith("|"):
            table_number += 1
            rows = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                row = lines[index].strip()
                if not re.fullmatch(r"[| :\-]+", row):
                    cells = re.split(r"(?<!\\)\|", row)[1:-1]
                    formatted = [inline(cell.strip().replace("\\|", "|")) for cell in cells]
                    formatted = [cell.replace("driverRatings|itemCount", "driverRatings|<br/>itemCount") for cell in formatted]
                    rows.append([Paragraph(cell, CELL) for cell in formatted])
                index += 1
            widths = [90, 155, 223] if len(rows[0]) == 3 else [135, 333]
            table = Table(rows, colWidths=widths, repeatRows=1, hAlign="LEFT")
            table.setStyle(TableStyle([
                ("LINEABOVE", (0, 0), (-1, 0), 0.8, colors.black),
                ("LINEBELOW", (0, 0), (-1, 0), 0.5, colors.black),
                ("LINEBELOW", (0, -1), (-1, -1), 0.8, colors.black),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
            ]))
            caption = "Data structures used by the system" if table_number == 1 else "Text file record formats"
            story.extend([Paragraph(f"Table {table_number}", HEADING),
                          Paragraph(f"<i>{caption}</i>", CAPTION), table, Spacer(1, 12)])
        else:
            bullet = re.match(r"^(-|\d+\.)\s+", line)
            block = [line[bullet.end():] if bullet else line]
            index += 1
            while index < len(lines) and lines[index].strip() and not re.match(r"^(#|\||- |\d+\. )", lines[index]):
                block.append(lines[index].strip())
                index += 1
            story.append(Paragraph(inline(" ".join(block)), LIST if bullet else BODY,
                                   bulletText=bullet.group(1) if bullet else None))

    for number, title, figure in [
        (1, "User roles and shared account behavior", user_figure()),
        (2, "Menu items and saved orders", order_figure()),
        (3, "Interfaces, delivery rules, and persistence", app_figure()),
    ]:
        story.extend([PageBreak(), Paragraph(f"Figure {number}", HEADING),
                      Paragraph(f"<i>{title}</i>", CAPTION), figure,
                      Paragraph("Note. Compact class view. Full-size PNG, SVG, and editable Excalidraw diagrams are included in docs/.", NOTE)])
    story.extend([PageBreak(), Paragraph("References", MAIN_HEADING),
                  Paragraph('Oracle. (n.d.-a). <i>Package javax.swing (Java SE 21 &amp; JDK 21).</i> '
                            'https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/package-summary.html', REFERENCE),
                  Paragraph('Oracle. (n.d.-b). <i>PriorityQueue (Java SE 17 &amp; JDK 17).</i> '
                            'https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/PriorityQueue.html', REFERENCE)])
    document = SimpleDocTemplate(str(OUTPUT), pagesize=(8.5 * inch, 11 * inch),
                                 leftMargin=inch, rightMargin=inch, topMargin=inch, bottomMargin=inch,
                                 title="CS233 Food Delivery System - Revised Design", author="Nobody Loves Java")
    document.build(story, onFirstPage=page_number, onLaterPages=page_number)
    print(f"Created {OUTPUT}")


if __name__ == "__main__":
    build_report()
