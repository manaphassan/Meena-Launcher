import os
import re

ui_dir = "app/src/main/java/my/hana/meena"

def snap_dp(val):
    v = float(val)
    if v <= 3:
        return '0.dp'
    elif v <= 9:
        return 'my.hana.meena.theme.MeenaSpacing.s'
    elif v <= 18:
        return 'my.hana.meena.theme.MeenaSpacing.m'
    else:
        return 'my.hana.meena.theme.MeenaSpacing.l'

def repl_spacing(match):
    # match.group(0) is the whole padding(...) or spacedBy(...) string
    inner = match.group(1)
    
    def repl_dp(dp_match):
        return snap_dp(dp_match.group(1))
    
    new_inner = re.sub(r'(\d+(?:\.\d+)?)\.dp', repl_dp, inner)
    return match.group(0).replace(inner, new_inner)

for root, _, files in os.walk(ui_dir):
    for f in files:
        if f.endswith('.kt'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8') as file:
                content = file.read()
            
            original_content = content
            
            # Snap spacing in padding and spacedBy
            content = re.sub(r'(?:padding|spacedBy)\s*\(([^)]*)\)', repl_spacing, content)
            
            # Fix font sizes < 12
            def repl_fontsize(match):
                size = float(match.group(1))
                if size < 12:
                    return 'fontSize = 12.sp'
                return match.group(0)
            content = re.sub(r'\bfontSize\s*=\s*(\d+(?:\.\d+)?)\.sp\b', repl_fontsize, content)
            
            # Fix ColorHex
            content = re.sub(r'\bColor\(0x[0-9a-fA-F]+\)', 'MaterialTheme.colorScheme.primary', content)
            
            # Fix ColorNamed
            content = re.sub(r'\bColor\.White\b', 'MeenaTextWhite', content)
            content = re.sub(r'\bColor\.Black\b', 'MeenaBlack', content)
            content = re.sub(r'\bColor\.Gray\b', 'MeenaTextMuted', content)
            content = re.sub(r'\bColor\.DarkGray\b', 'MeenaTextMuted', content)
            content = re.sub(r'\bColor\.LightGray\b', 'MeenaTextMuted', content)
            
            # Fix FontWeight
            content = re.sub(r'\bFontWeight\.Bold\b', 'FontWeight.Medium', content)
            content = re.sub(r'\bFontWeight\.SemiBold\b', 'FontWeight.Normal', content)
            content = re.sub(r'\bFontWeight\.ExtraBold\b', 'FontWeight.Medium', content)
            content = re.sub(r'\bFontWeight\.Black\b', 'FontWeight.Medium', content)
            
            if content != original_content:
                with open(path, 'w', encoding='utf-8') as file:
                    file.write(content)
                print(f"Updated {path}")
