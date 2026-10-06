def fix_canvas():
    path = "app/src/main/java/my/hana/meena/ui/components/DynamicSolatCelestialCard.kt"
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    in_canvas = False
    for i in range(len(lines)):
        if 'Canvas(modifier = modifier) {' in lines[i]:
            in_canvas = True
        
        # Assume canvas block ends around line 660, or just look for the end of the composable 
        # Actually it's easier to just stop when we see another @Composable or something.
        if in_canvas and 'MaterialTheme.colorScheme.primary' in lines[i]:
            lines[i] = lines[i].replace('MaterialTheme.colorScheme.primary', 'primaryColor')
            
        if in_canvas and '@Composable' in lines[i]:
            in_canvas = False
            
    with open(path, 'w', encoding='utf-8') as f:
        f.writelines(lines)

fix_canvas()
