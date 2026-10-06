import re

path = 'app/src/main/java/my/hana/meena/theme/Theme.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('baseTheme: String = "amoled",', 'baseTheme: String = "dark",')
c = re.sub(r'"slate" -> darkColorScheme\([\s\S]*?\)\n    else', 'else', c)
c = c.replace('fontFamilyName: String = "Segoe UI",\n', '')
c = c.replace('fontFamilyName = fontFamilyName,\n', '')
c = c.replace('fontFamilyName, fontScalePercent', 'fontScalePercent')

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)

