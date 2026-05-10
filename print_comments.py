import glob

files = glob.glob('app/src/main/java/com/oficial/viasit/data/**/*.kt', recursive=True)

for filepath in files:
    with open(filepath, 'r') as f:
        lines = f.readlines()
    for i, line in enumerate(lines):
        if line.strip().startswith('//'):
            print(f"{filepath}:{i+1}: {line.strip()}")
