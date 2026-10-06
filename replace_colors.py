import os
import re

ui_dir = "app/src/main/java/my/hana/meena/ui"
replacements = [
    (r'Color\.White\.copy\(alpha = ([0-9.]+)f\)', r'MeenaTextWhite.copy(alpha = \1f)'),
    (r'Color\.White', 'MeenaTextWhite'),
    (r'Color\.Black', 'MeenaBlack'),
    (r'Color\.Gray', 'MeenaTextMuted'),
    (r'Color\(0xFF333333\)', 'MaterialTheme.colorScheme.surfaceVariant'),
    (r'Color\(0xFF222222\)', 'MaterialTheme.colorScheme.surfaceVariant'),
    (r'Color\(0xFF444444\)', 'MaterialTheme.colorScheme.surfaceVariant'),
    (r'Color\(0xFF555555\)', 'MaterialTheme.colorScheme.outline'),
    (r'Color\(0xFFFF5252\)', 'MaterialTheme.colorScheme.error'),
    (r'Color\(0xFFFF6B6B\)', 'MaterialTheme.colorScheme.error'),
    (r'Color\(0xFFE51400\)', 'MaterialTheme.colorScheme.error'),
    (r'Color\(0xFF25D366\)', 'MeenaProfitGreen'),
    (r'Color\(0xFFEA4335\)', 'MaterialTheme.colorScheme.error'),
    (r'Color\(0xFF0088CC\)', 'MaterialTheme.colorScheme.primary'),
    (r'Color\(0xFF[0-9A-Fa-f]{6}\)', 'MaterialTheme.colorScheme.primary') # Catch all remaining hardcoded hexes
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
            
            # Remove Color imports if not used
            if 'import androidx.compose.ui.graphics.Color\n' in content and 'Color(' not in content and 'Color.' not in content:
                content = content.replace('import androidx.compose.ui.graphics.Color\n', '')

            # Add MaterialTheme import if missing
            if 'MaterialTheme' in content and 'import androidx.compose.material3.MaterialTheme' not in content:
                content = content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.material3.MaterialTheme\nimport androidx.compose.ui.Modifier')
            
            if 'MeenaTextWhite' in content and 'import my.hana.meena.theme.MeenaTextWhite' not in content:
                content = content.replace('import my.hana.meena.theme.MeenaTextMuted', 'import my.hana.meena.theme.MeenaTextMuted\nimport my.hana.meena.theme.MeenaTextWhite')
                content = content.replace('import my.hana.meena.theme.MeenaBlack', 'import my.hana.meena.theme.MeenaBlack\nimport my.hana.meena.theme.MeenaTextWhite')
            
            if 'MeenaBlack' in content and 'import my.hana.meena.theme.MeenaBlack' not in content:
                content = content.replace('import my.hana.meena.theme.MeenaTextWhite', 'import my.hana.meena.theme.MeenaTextWhite\nimport my.hana.meena.theme.MeenaBlack')

            if 'MeenaProfitGreen' in content and 'import my.hana.meena.theme.MeenaProfitGreen' not in content:
                content = content.replace('import my.hana.meena.theme.MeenaTextWhite', 'import my.hana.meena.theme.MeenaProfitGreen\nimport my.hana.meena.theme.MeenaTextWhite')

            if content != original_content:
                with open(path, 'w', encoding='utf-8') as file:
                    file.write(content)
                print(f"Updated {path}")
