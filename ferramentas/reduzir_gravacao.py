"""Reduz uma gravação do modo detetive para virar teste automático.

Fica só o que o bloqueio usa: o nome (id) de cada peça da tela, se está visível, se está
selecionada e onde está. Nenhum texto, descrição ou nome de pessoa passa.

Uso: python ferramentas/reduzir_gravacao.py <gravacao.txt> <saida.jsonl>
"""
import json
import sys


def pecas(no, saida):
    if "id" in no:
        saida.append([no["id"], 0 if no.get("v") == 0 else 1, 1 if no.get("s") else 0, no["b"]])
    for filho in no.get("n", []):
        pecas(filho, saida)


def reduzir(linha):
    d = json.loads(linha)
    if "cen" not in d:  # cabeçalho
        return {k: d.get(k) for k in ("instagram", "android", "tela")}
    r = {"cen": d["cen"], "t": d["t"]}
    if "foto" in d:
        r["foto"] = d["foto"]
        nos = []
        for j in d["janelas"]:
            if j.get("pacote") == "com.instagram.android" and "raiz" in j:
                pecas(j["raiz"], nos)
        r["nos"] = nos
    else:
        r["ev"] = d["ev"]
        if "fonte" in d and "id" in d["fonte"]:
            r["id"] = d["fonte"]["id"]
        for k in ("de", "ate"):
            if k in d:
                r[k] = d[k]
    return r


def main():
    entrada, saida = sys.argv[1], sys.argv[2]
    with open(entrada, encoding="utf-8") as e, open(saida, "w", encoding="utf-8", newline="\n") as s:
        for linha in e:
            if linha.strip():
                s.write(json.dumps(reduzir(linha), ensure_ascii=False, separators=(",", ":")) + "\n")


if __name__ == "__main__":
    main()
