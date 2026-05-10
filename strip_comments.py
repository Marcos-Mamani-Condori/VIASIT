import glob
import re

files = glob.glob('app/src/main/java/com/oficial/viasit/domain/model/*.kt')

for filepath in files:
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    new_lines = []
    in_docstring = False
    for line in lines:
        stripped = line.strip()
        
        # We want to remove AI-like comments above fields. 
        # Usually they start with //
        if stripped.startswith('//'):
            continue
            
        new_lines.append(line)
        
    with open(filepath, 'w') as f:
        f.writelines(new_lines)
    print(f"Cleaned {filepath}")
