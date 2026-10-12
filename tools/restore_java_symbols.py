#!/usr/bin/env python3
import os
import glob
import re

def find_matching_brace(text, start_idx):
    depth = 1
    i = start_idx
    n = len(text)
    in_string = False
    in_char = False
    in_line_comment = False
    in_block_comment = False
    
    while i < n and depth > 0:
        c = text[i]
        if in_line_comment:
            if c == "\n":
                in_line_comment = False
        elif in_block_comment:
            if c == "*" and i + 1 < n and text[i+1] == "/":
                in_block_comment = False
                i += 1
        elif in_string:
            if c == "\\" and i + 1 < n:
                i += 1
            elif c == "\"":
                in_string = False
        elif in_char:
            if c == "\\" and i + 1 < n:
                i += 1
            elif c == "\x27":
                in_char = False
        else:
            if c == "/" and i + 1 < n and text[i+1] == "/":
                in_line_comment = True
                i += 1
            elif c == "/" and i + 1 < n and text[i+1] == "*":
                in_block_comment = True
                i += 1
            elif c == "\"":
                in_string = True
            elif c == "\x27":
                in_char = True
            elif c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    return i
        i += 1
    return -1

def extract_cs_methods(cs_code):
    methods = {}
    pattern = r"(?:(?:public|protected|private|static|virtual|override|new|internal)\s+)*(?:(?:[\w<>\[\],?]+)\s+)?([A-Za-z0-9_]+)\s*\(([^)]*)\)\s*\{"
    for m in re.finditer(pattern, cs_code):
        name = m.group(1)
        if name in ["if", "while", "for", "switch", "catch", "using", "lock"]:
            continue
        params_str = m.group(2).strip()
        params = []
        if params_str:
            for p in params_str.split(","):
                parts = p.strip().split()
                if len(parts) >= 2:
                    params.append((parts[-2], parts[-1]))
        methods[(name, len(params))] = params
    return methods

def replace_in_code_preserving_strings(code, replacements):
    # replacements is list of (old_token, new_token)
    # Tokenize code to avoid modifying strings/comments
    token_pattern = r"(\"(?:\\.|[^\"\\])*\"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/|\b[a-zA-Z_0-9]+\b)"
    
    rep_map = dict(replacements)
    
    def replacer(match):
        tok = match.group(0)
        if tok.startswith("\"") or tok.startswith("\'") or tok.startswith("//") or tok.startswith("/*"):
            return tok
        return rep_map.get(tok, tok)
        
    return re.sub(token_pattern, replacer, code)

def process_file(jf_path, cf_path):
    with open(jf_path, "r", encoding="utf-8") as f:
        j_text = f.read()
    with open(cf_path, "r", encoding="utf-8") as f:
        c_text = f.read()
        
    cs_methods = extract_cs_methods(c_text)
    
    # Global replacement of common decompiler artifact names
    # e.g., mGraphics mGraphics2 -> mGraphics g
    
    # Find all Java methods and constructors
    method_pattern = r"((?:(?:public|protected|private|static|final|abstract|synchronized)\s+)+(?:(?:[\w<>\[\],?]+)\s+)?([A-Za-z0-9_]+)\s*\(([^)]*)\)\s*\{)"
    
    # We will process methods from end of file to start so indices remain valid
    matches = list(re.finditer(method_pattern, j_text))
    matches.sort(key=lambda m: m.start(), reverse=True)
    
    total_renamed = 0
    
    for m in matches:
        header_full = m.group(1)
        name = m.group(2)
        params_str = m.group(3).strip()
        if name in ["if", "while", "for", "switch", "catch"]:
            continue
            
        params = []
        if params_str:
            for p in params_str.split(","):
                parts = p.strip().split()
                if len(parts) >= 2:
                    params.append((parts[-2], parts[-1]))
                    
        key = (name, len(params))
        
        # Check if we have C# counterpart
        cs_params = cs_methods.get(key)
        
        # Find body
        brace_start = m.end() - 1
        brace_end = find_matching_brace(j_text, brace_start + 1)
        if brace_end == -1:
            continue
            
        header = j_text[m.start():brace_start]
        body = j_text[brace_start + 1:brace_end]
        
        replacements = []
        
        if cs_params and len(cs_params) == len(params):
            for (jt, jn), (ct, cn) in zip(params, cs_params):
                # Clean C# param name (strip @ prefix if any)
                cn_clean = cn.lstrip("@")
                if jn != cn_clean and re.match(r"^[a-zA-Z_][a-zA-Z0-9_]*$", cn_clean):
                    replacements.append((jn, cn_clean))
        else:
            # Fallback for mGraphics parameter
            for jt, jn in params:
                if jt == "mGraphics" and jn.startswith("mGraphics"):
                    replacements.append((jn, "g"))
                    
        if replacements:
            # Rename in header and body
            new_header = replace_in_code_preserving_strings(header, replacements)
            new_body = replace_in_code_preserving_strings(body, replacements)
            j_text = j_text[:m.start()] + new_header + "{" + new_body + "}" + j_text[brace_end + 1:]
            total_renamed += len(replacements)
            
    # Also replace abstract/interface method declarations without bodies
    # e.g., void paint(mGraphics mGraphics2); -> void paint(mGraphics g);
    j_text = re.sub(r"\bmGraphics\s+mGraphics\d+\b", "mGraphics g", j_text)
    
    with open(jf_path, "w", encoding="utf-8") as f:
        f.write(j_text)
        
    return total_renamed

def main():
    java_files = glob.glob("src_java/**/*.java", recursive=True)
    cs_files = {os.path.basename(f)[:-3]: f for f in glob.glob("src_csharp/**/*.cs", recursive=True)}
    
    total_files_modified = 0
    total_params = 0
    
    for jf in java_files:
        cname = os.path.basename(jf)[:-5]
        if cname in cs_files:
            renamed = process_file(jf, cs_files[cname])
            if renamed > 0:
                total_files_modified += 1
                total_params += renamed
        else:
            # Still clean mGraphics
            with open(jf, "r", encoding="utf-8") as f:
                content = f.read()
            new_content = re.sub(r"\bmGraphics\s+mGraphics\d+\b", "mGraphics g", content)
            if new_content != content:
                with open(jf, "w", encoding="utf-8") as f:
                    f.write(new_content)
                total_files_modified += 1
                
    print(f"Hoàn thành! Đã đối chiếu và khôi phục {total_params} tham số trên {total_files_modified} file Java.")

if __name__ == "__main__":
    main()
