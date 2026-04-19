import os
import re
from whoosh import index
from whoosh.fields import Schema, TEXT, ID
from whoosh.analysis import StemmingAnalyzer, NgramFilter

# ---------------------------------------------------------
# 1. Define the Whoosh schema
# ---------------------------------------------------------
# title: stored and indexed
# content: full text of the Wikipedia page

schema = Schema(
    title=ID(stored=True, unique=True),
    content=TEXT(stored=False, analyzer=StemmingAnalyzer() | NgramFilter(minsize=2, maxsize=2))
)

# ---------------------------------------------------------
# 2. Create index directory
# ---------------------------------------------------------
INDEX_DIR = "wiki_index"

if not os.path.exists(INDEX_DIR):
    os.mkdir(INDEX_DIR)
    ix = index.create_in(INDEX_DIR, schema)
else:
    ix = index.open_dir(INDEX_DIR)

# Modified for silly, sad Windows computer :(
writer = ix.writer(limitmb=512)
#writer = ix.writer(limitmb=512, procs=4, multisegment=True)

# ---------------------------------------------------------
# 3. Function to parse Wikipedia pages
# ---------------------------------------------------------
# Each page begins with a line like: [[Page Title]]
# Followed by the page content until the next [[Title]]
page_title_pattern = re.compile(r"""
    ^
    
    \[
        
    \[(.+?)\]
    
    \]
    $
""", re.VERBOSE)

def parse_wiki_file(filepath):
    """
    Generator that yields (title, content) tuples for each Wikipedia page.
    """
    with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
        title = None
        content_lines = []

        for line in f:
            line = line.rstrip("\n")

            # Detect start of a new page
            match = page_title_pattern.match(line)
            if match:
                # If we were collecting a previous page, yield it
                if title is not None:
                    yield (title, "\n".join(content_lines))

                # Start new page
                title = match.group(1)
                content_lines = []
            else:
                # Accumulate content
                if title is not None:
                    content_lines.append(line)

        # Yield last page in file
        if title is not None:
            yield (title, "\n".join(content_lines))

# ---------------------------------------------------------
# 4. Index all Wikipedia files
# ---------------------------------------------------------
WIKI_DIR = "wiki-subset-20140602"   # directory containing the 80 files

files = sorted(
    f for f in os.listdir(WIKI_DIR)
    if not f.startswith("._") and not f.startswith(".")
)

for filename in files:
    filepath = os.path.join(WIKI_DIR, filename)
    print(f"Indexing file: {filename}")

    for title, content in parse_wiki_file(filepath):
        writer.add_document(title=title, content=content)

# ---------------------------------------------------------
# 5. Commit index
# ---------------------------------------------------------
writer.commit()
print("Indexing complete!")
