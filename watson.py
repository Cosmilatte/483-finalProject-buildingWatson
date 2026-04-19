from whoosh import index
from whoosh.qparser import QueryParser
from whoosh.scoring import BM25F

def load_jeopardy_questions(filepath):
    """
    Reads a Jeopardy question file where each question is 4 lines:
    CATEGORY
    CLUE
    ANSWER
    (blank line)

    Returns a list of dicts with keys: category, clue, answer.
    """
    questions = []

    with open(filepath, "r", encoding="utf-8") as f:
        lines = [line.rstrip("\n") for line in f]

    i = 0
    n = len(lines)

    while i < n:
        # Skip any accidental empty lines
        if lines[i].strip() == "":
            i += 1
            continue

        category = lines[i].strip()
        clue = lines[i+1].strip()
        answer = lines[i+2].strip()

        questions.append({
            "category": category,
            "clue": clue,
            "answer": answer
        })

        # Move to next block (skip blank line)
        i += 4

    return questions

def load_searcher(index_dir="wiki_index"):
    """
    Opens the Whoosh index and returns a searcher + query parser.
    """
    ix = index.open_dir(index_dir)
    weighting = BM25F(title_B=0.3, content_B=0.75, title_weight=8.0)
    searcher = ix.searcher(weighting=BM25F(title_B=2.0, content_B=0.75, title_weight=3.0))
    parser = QueryParser("content", schema=ix.schema)
    return searcher, parser


def retrieve_best_page(clue, searcher, parser, top_k=1):
    """
    Given a Jeopardy clue, returns the title of the most relevant Wikipedia page.
    """
    query = parser.parse(clue)
    results = searcher.search(query, limit=top_k)

    if len(results) == 0:
        return "None"  # no match found

    return results[0]["title"]


def retrieve_top_k(clue, searcher, parser, k=5):
    """
    Returns the top-k most similar Wikipedia page titles.
    """
    query = parser.parse(clue)
    results = searcher.search(query, limit=k)
    return [r["title"] for r in results]

def main():
    questions = load_jeopardy_questions("questions.txt")
    
    searcher, parser = load_searcher()
    correct = 0
    total = 0
    #NEWSPAPERS
    for q in questions:
        clue = q['clue']
        answer = q['answer']
        
        category = q['category']
        USELESS_CATEGORIES = {
        "POTPOURRI", "GRAB BAG", "MISCELLANEOUS",
        "ODDS & ENDS", "HODGEPODGE", "VARIETY"
        }
        if category.upper() in USELESS_CATEGORIES:
            query_text = clue
        else:
            query_text = f"{category} {clue}"
        
        # if can get categories working later on, use query_text instead of clue
        best = retrieve_best_page(clue, searcher, parser)
        if best.lower() == answer.lower():
            correct += 1
        total += 1
    print("Correct:", correct)
    print("Total:", total)
    #print("Predicted answer:", best)

if __name__ == "__main__":
    main()
    
'''
for q in questions:
        clue = q['clue']
        answer = q['answer']
        
        category = q['category']
        USELESS_CATEGORIES = {
        "POTPOURRI", "GRAB BAG", "MISCELLANEOUS",
        "ODDS & ENDS", "HODGEPODGE", "VARIETY"
        }
        if category.upper() in USELESS_CATEGORIES:
            query_text = clue
        else:
            query_text = f"{category} {clue}"
        
        # if can get categories working later on, use query_text instead of clue
        best = retrieve_best_page(clue, searcher, parser)
        if best.lower() == answer.lower():
            correct += 1
        total += 1
'''
