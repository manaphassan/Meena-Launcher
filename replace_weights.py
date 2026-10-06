import os
import re

ui_dir = "app/src/main/java/my/hana/meena/ui"
replacements = [
    (r'FontWeight\.Bold', 'FontWeight.Medium'),
    (r'FontWeight\.SemiBold', 'FontWeight.Normal'),
    (r'FontWeight\.ExtraLight', 'FontWeight.Light')
]

for root, _, files in os.walk(ui_dir):
    for f in files:
        if f.endswith('.kt'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8') as file:
                content = file.read()
            
            original_content = content
            for old, new in replacements:
                content = re.sub(old, new, content)
            
            if content != original_content:
                with open(path, 'w', encoding='utf-8') as file:
                    file.write(content)
                print(f"Updated font weights in {path}")
