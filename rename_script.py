import os
import glob

# Mapping
col_replacements = {
    '"autos"': '"vehicles"',
    '"lineas"': '"lines"',
    '"rutas"': '"routes"',
    '"reportes"': '"reports"'
}

files_to_check = glob.glob('app/src/main/java/com/oficial/viasit/data/**/*.kt', recursive=True)

for filepath in files_to_check:
    with open(filepath, 'r') as f:
        content = f.read()
    
    new_content = content
    for old, new in col_replacements.items():
        new_content = new_content.replace(old, new)
        
    if content != new_content:
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

