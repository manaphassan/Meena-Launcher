import os
import re

ui_dir = "app/src/main/java/my/hana/meena/ui"
replacements = [
    (r'\b6\.dp\b', 'my.hana.meena.theme.MeenaSpacing.s'),
    (r'\b12\.dp\b', 'my.hana.meena.theme.MeenaSpacing.m'),
    (r'\b24\.dp\b', 'my.hana.meena.theme.MeenaSpacing.l')
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
            
            # Need to fix any potential "my.hana.meena.theme.MeenaSpacing.s" without import? 
            # I hardcoded the fully qualified name, so it's safe without imports! 
            # Or I could import it. Fully qualified is safer for regex replacement.
            # Actually, Jetpack Compose doesn't mind fully qualified names.
            
            if content != original_content:
                with open(path, 'w', encoding='utf-8') as file:
                    file.write(content)
                print(f"Updated spacing in {path}")
