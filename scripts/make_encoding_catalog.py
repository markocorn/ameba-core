"""Write the weekend experiment: one hundred encodings of the same graph.

Every encoding is a :class:`ameba_genome.fixed.FixedConfig` -- a fixed-role
genome whose numbers are read through a different map, mutated by different
operators, or shaped differently -- so all of them run under one GA on one
plant and differ only in how the genome stands for the graph. Three reference
series are added: the two best plain fixed encodings so far and the
context-aware free sequence.

    python scripts/make_encoding_catalog.py
    python scripts/run_experiments.py experiments/weekend-encodings.json

The groups, in order: number transforms, per-role mixes, analogies from
nature, mutation operators, record shape and addressing, and a few chimeras.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from ameba_genome import Grammar  # noqa: E402
from ameba_genome.fixed import FixedConfig, FixedEncoding  # noqa: E402

ALL = ("kind", "parameter", "source", "weight")
STRUCTURE = ("kind", "source")
VALUES = ("parameter", "weight")


def every(layout: str, roles=ALL) -> dict:
    return {role: layout for role in roles}


NATURE_MIX = {"creep": 0.35, "reset": 0.2, "insert_record": 0.1, "remove_record": 0.1,
              "duplicate_record": 0.1, "transpose": 0.05, "invert": 0.05, "burst": 0.05}

# (key, group, label, description, config, extra harness args)
CATALOG: list[tuple] = []


def add(key, group, label, desc, config, args=()):
    CATALOG.append((key, group, label, desc, config, list(args)))


# --- 1. Number transforms on every role ---------------------------------------
G = "preslikave števil"
add("t-runs64", G, "nizi, širina 64", "Vsako mesto 0–63 v nizih sinonimov: malo nevtralnosti.", {"layouts": every("runs"), "width": 64})
add("t-runs256", G, "nizi, širina 256", "Vsako mesto 0–255 v nizih.", {"layouts": every("runs"), "width": 256})
add("t-runs4096", G, "nizi, širina 4096", "Vsako mesto 0–4095 v nizih: zelo nevtralno.", {"layouts": every("runs"), "width": 4096})
add("t-shuffle64", G, "mešano, širina 64", "Pomešani sinonimi, malo jih je.", {"layouts": every("shuffle"), "width": 64})
add("t-xor21", G, "XOR 21", "Število XOR 21, nato nizi (širina 1024).", {"layouts": every("xor:21"), "width": 1024})
add("t-xor341", G, "XOR 341", "Število XOR 341 (izmenični biti).", {"layouts": every("xor:341"), "width": 1024})
add("t-xor1023", G, "XOR 1023", "Vsi biti obrnjeni: zrcalna slika.", {"layouts": every("xor:1023"), "width": 1024})
add("t-mul7", G, "množenje ×7", "n·7 mod 1024: koraki po 7 nizov.", {"layouts": every("mul:7"), "width": 1024})
add("t-mul333", G, "množenje ×333", "n·333 mod 1024: grobo premešano s strukturo.", {"layouts": every("mul:333"), "width": 1024})
add("t-gray", G, "Grayeva koda", "Grayevo dekodiranje, mutacija ±1.", {"layouts": every("gray"), "width": 1024})
add("t-gray-bits", G, "Gray + preklop bita", "Boolovo polje v Grayevi kodi, mutacija preklopi bit.", {"layouts": every("gray"), "width": 1024, "mutation": {"bitflip": 0.5, "reset": 0.3, "insert_record": 0.1, "remove_record": 0.1}})
add("t-binary-bits", G, "binarno + preklop bita", "Navadna binarna koda, mutacija preklopi bit.", {"layouts": every("runs"), "width": 1024, "mutation": {"bitflip": 0.5, "reset": 0.3, "insert_record": 0.1, "remove_record": 0.1}})
add("t-sin001", G, "sinus, C=0,01", "Počasen sinus: gladka, a ponavljajoča se preslikava.", {"layouts": every("sin:0.01"), "width": 1000})
add("t-sin01", G, "sinus, C=0,1", "Hitrejši sinus: kratke periode.", {"layouts": every("sin:0.1"), "width": 1000})
add("t-sin1", G, "sinus, C=1", "Sinus z eno radiansko periodo na korak: skoraj naključno.", {"layouts": every("sin:1"), "width": 1000})
add("t-sin37", G, "sinus, C=37", "Kaotičen sinus.", {"layouts": every("sin:37"), "width": 1000})
add("t-square", G, "kvadrat mod", "n² mod 1024: nelinearna, ne-bijektivna.", {"layouts": every("sq"), "width": 1024})
add("t-logistic36", G, "logistična 3,6", "20 korakov logistične preslikave pri r=3,6: na robu kaosa.", {"layouts": every("logistic:3.6"), "width": 1000})
add("t-logistic399", G, "logistična 3,99", "Logistična preslikava v polnem kaosu.", {"layouts": every("logistic:3.99"), "width": 1000})
add("t-popcount-bits", G, "popcount + preklop bita", "Simbol = število enic; preklop bita ga spremeni za ±1.", {"layouts": every("popcount"), "width": 1024, "mutation": {"bitflip": 0.5, "reset": 0.3, "insert_record": 0.1, "remove_record": 0.1}})
add("t-popcount", G, "popcount + ±1", "Simbol = število enic, mutacija ±1.", {"layouts": every("popcount"), "width": 1024})
add("t-digits", G, "vsota števk", "Simbol = vsota desetiških števk mod abeceda.", {"layouts": every("digits"), "width": 1000})
add("t-collatz", G, "Collatz", "Simbol = Collatzovo število korakov: skoraj kaotično.", {"layouts": every("collatz"), "width": 1000})
add("t-integral", G, "integral, vse vloge", "Simbol = tekoča vsota vseh prejšnjih mest iste vloge.", {"layouts": every("cumulative")})
add("t-integral-struct", G, "integral, struktura", "Tekoča vsota samo za tip in vir.", {"layouts": {**every("runs"), **every("cumulative", STRUCTURE)}})
add("t-integral-values", G, "integral, vrednosti", "Tekoča vsota samo za parametre in uteži.", {"layouts": {**every("runs"), **every("cumulative", VALUES)}})
add("t-derivative", G, "diferenca, vse vloge", "Simbol = razlika do prejšnjega mesta iste vloge.", {"layouts": every("difference")})
add("t-derivative-struct", G, "diferenca, struktura", "Diferenca samo za tip in vir.", {"layouts": {**every("runs"), **every("difference", STRUCTURE)}})
add("t-golden", G, "zlati rez, 1000", "Branje z zlatim rezom (Weylovo zaporedje).", {"layouts": every("golden"), "width": 1000})
add("t-golden4096", G, "zlati rez, 4096", "Zlati rez na širšem prostoru.", {"layouts": every("golden"), "width": 4096})
add("t-gradient1", G, "gradient 1", "Pomen se premakne za 1 z vsakim zapisom.", {"layouts": every("gradient:1")})
add("t-gradient37", G, "gradient 37", "Pomen se premakne za 37 z vsakim zapisom.", {"layouts": every("gradient:37")})
add("t-gradient-kind", G, "gradient samo tip", "Gradient samo na tipu vozla.", {"layouts": {**every("runs"), "kind": "gradient:13"}})

# --- 2. Per-role mixes -----------------------------------------------------------
G = "mešanice po vlogah"
add("m-shuffle-log", G, "tip mešan, uteži log", "Tip pomešan, vir in parameter v nizih, uteži logaritemsko.", {"layouts": {"kind": "shuffle", "source": "runs", "parameter": "runs", "weight": "log"}})
add("m-gray-fine", G, "tip Gray, uteži fine", "Tip v Grayevi kodi, ostalo v nizih, fine uteži.", {"layouts": {"kind": "gray", "source": "runs", "parameter": "runs", "weight": "fine"}, "width": 1024})
add("m-sin-kind", G, "tip kaotičen sinus", "Kaotičen sinus samo za tip, ostalo v nizih.", {"layouts": {**every("runs"), "kind": "sin:37"}})
add("m-xor-source", G, "vir XOR", "XOR samo za vir, ostalo v nizih.", {"layouts": {**every("runs"), "source": "xor:341"}, "width": 1024})
add("m-golden-source", G, "vir zlati rez", "Zlati rez samo za vir.", {"layouts": {**every("runs"), "source": "golden"}})
add("m-base-log", G, "base + log uteži", "Kot A-base, a uteži na logaritemski lestvici.", {"layouts": {**every("direct"), "weight": "log"}})
add("m-base-fine", G, "base + fine uteži", "Kot A-base, a fine linearne uteži.", {"layouts": {**every("direct"), "weight": "fine"}})
add("m-runs-log", G, "nizi + log uteži", "Kot A-runs, a logaritemske uteži.", {"layouts": {**every("runs"), "weight": "log"}})
add("m-runs-log4096", G, "nizi 4096 + log uteži", "Široki nizi in logaritemske uteži.", {"layouts": {**every("runs"), "weight": "log"}, "width": 4096})
add("m-integral-kind", G, "integral samo tip", "Tekoča vsota samo za tip, ostalo neposredno.", {"layouts": {**every("direct"), "kind": "cumulative"}})
add("m-derivative-weight", G, "diferenca samo uteži", "Uteži kot razlika do prejšnje uteži.", {"layouts": {**every("runs"), "weight": "difference"}})

# --- 3. Analogies from nature ------------------------------------------------------
G = "analogije iz narave"
add("n-codon-base", G, "genetski kod + točkovne", "Vsako mesto je kodon; mutacija spremeni bazo, tranzicije 2×.", {"layouts": every("codon"), "mutation": {"base": 0.5, "reset": 0.2, "insert_record": 0.15, "remove_record": 0.15}})
add("n-codon-creep", G, "genetski kod + ±1", "Kodoni, a mutacija ±1 po indeksu kodona.", {"layouts": every("codon")})
add("n-codon-kind", G, "kodon samo za tip", "Tip je kodon (stop utiša vozel), ostalo v nizih.", {"layouts": {**every("runs"), "kind": "codon"}, "mutation": {"base": 0.3, "creep": 0.3, "reset": 0.2, "insert_record": 0.1, "remove_record": 0.1}})
add("n-codon-structure", G, "kodon za strukturo, log uteži", "Tip in vir sta kodona, uteži logaritemske.", {"layouts": {"kind": "codon", "source": "codon", "parameter": "runs", "weight": "log"}, "mutation": {"base": 0.4, "creep": 0.2, "reset": 0.2, "insert_record": 0.1, "remove_record": 0.1}})
add("n-diploid", G, "diploidno, dominanca", "Dva alela na mesto, izrazi se večji. Recesivne mutacije so skrite.", {"layouts": every("diploid:32")})
add("n-diploid-bits", G, "diploidno + preklop bita", "Diploidno, mutacija preklopi bit enega alela.", {"layouts": every("diploid:32"), "mutation": {"bitflip": 0.5, "reset": 0.3, "insert_record": 0.1, "remove_record": 0.1}})
add("n-codominant", G, "kodominanca", "Dva alela, izrazi se povprečje.", {"layouts": every("codominant:32")})
add("n-diploid-structure", G, "diploidna struktura, fine uteži", "Diploiden tip in vir, fine uteži.", {"layouts": {"kind": "diploid:32", "source": "diploid:32", "parameter": "runs", "weight": "fine"}})
add("n-majority10", G, "kanalizacija, 3 kopije po 10", "Tri kopije na mesto, izrazi se večina.", {"layouts": every("majority:10")})
add("n-majority6", G, "kanalizacija, 3 kopije po 6", "Manjša abeceda kopij.", {"layouts": every("majority:6")})
add("n-majority-kind", G, "kanalizacija samo tip", "Tip robusten z večino, ostalo v nizih.", {"layouts": {**every("runs"), "kind": "majority:10"}})
add("n-epigenetic", G, "epigenetsko utišanje", "Polovica vrednosti tipa vozel utiša (metilacija).", {"layouts": {**every("runs"), "kind": "epigenetic"}})
add("n-epigenetic-log", G, "utišanje + log uteži", "Epigenetsko utišanje in logaritemske uteži.", {"layouts": {"kind": "epigenetic", "source": "runs", "parameter": "runs", "weight": "log"}})
add("n-tag", G, "ključ in ključavnica", "Vir se veže na prejšnji vozel z najbližjo oznako.", {"addressing": "tag"})
add("n-tag-runs", G, "ključ in ključavnica, nizi", "Vezava po oznakah, mesta v nizih.", {"addressing": "tag", "layouts": {**every("runs"), "tag": "runs"}})
add("n-tag-3", G, "vezava, 3 viri", "Vezava po oznakah, trije viri na vozel.", {"addressing": "tag", "sources": 3})
add("n-tag-sum", G, "vezava, izhod vsota", "Vezava po oznakah, izhod sešteje dva vira.", {"addressing": "tag", "output_sources": 2})
add("n-sunflower-morphogen", G, "sončnica + morfogen", "Tip z zlatim rezom, vir z gradientom.", {"layouts": {"kind": "golden", "source": "gradient:7", "parameter": "runs", "weight": "runs"}})

# --- 4. Mutation operators -----------------------------------------------------------
G = "mutacijski operatorji"
add("o-duplicate", G, "podvojitev gena", "A-base z mutacijo, ki kopira zapis.", {"mutation": {"creep": 0.4, "reset": 0.25, "insert_record": 0.1, "remove_record": 0.1, "duplicate_record": 0.15}})
add("o-duplicate-runs", G, "podvojitev gena, nizi", "A-runs s podvojitvijo zapisov.", {"layouts": every("runs"), "mutation": {"creep": 0.4, "reset": 0.25, "insert_record": 0.1, "remove_record": 0.1, "duplicate_record": 0.15}})
add("o-transpose", G, "skakajoči geni", "A-base s premikom zapisov.", {"mutation": {"creep": 0.4, "reset": 0.25, "insert_record": 0.1, "remove_record": 0.1, "transpose": 0.15}})
add("o-invert", G, "inverzija", "A-base z obratom bloka zapisov.", {"mutation": {"creep": 0.4, "reset": 0.25, "insert_record": 0.1, "remove_record": 0.1, "invert": 0.15}})
add("o-burst", G, "sevanje", "A-base z izbruhi več mutacij.", {"mutation": {"creep": 0.4, "reset": 0.25, "insert_record": 0.1, "remove_record": 0.1, "burst": 0.15}})
add("o-burst-runs", G, "sevanje, nizi", "A-runs z izbruhi.", {"layouts": every("runs"), "mutation": {"creep": 0.4, "reset": 0.25, "insert_record": 0.1, "remove_record": 0.1, "burst": 0.15}})
add("o-nature", G, "vsi naravni operatorji", "A-base z vsemi naravnimi mutacijami.", {"mutation": NATURE_MIX})
add("o-nature-runs", G, "vsi naravni operatorji, nizi", "A-runs z vsemi naravnimi mutacijami.", {"layouts": every("runs"), "mutation": NATURE_MIX})
add("o-hot-weight", G, "vroča točka: uteži", "Uteži mutirajo 5× pogosteje.", {"role_rates": {"kind": 1, "parameter": 1, "source": 1, "weight": 5}})
add("o-hot-kind", G, "vroča točka: tip", "Tip vozla mutira 5× pogosteje.", {"role_rates": {"kind": 5, "parameter": 1, "source": 1, "weight": 1}})
add("o-hot-source", G, "vroča točka: vir, nizi", "Viri mutirajo 5× pogosteje, A-runs.", {"layouts": every("runs"), "role_rates": {"kind": 1, "parameter": 1, "source": 5, "weight": 1}})
add("o-creep-only", G, "samo ±1", "A-base, brez naključnih zamenjav.", {"mutation": {"creep": 0.8, "insert_record": 0.1, "remove_record": 0.1}})
add("o-reset-only", G, "samo zamenjava", "A-base, brez ±1.", {"mutation": {"reset": 0.8, "insert_record": 0.1, "remove_record": 0.1}})
add("o-geometric", G, "geometrijski koraki", "A-runs, korak ±1 z daljšim repom (p=0,5).", {"layouts": every("runs"), "step": "geometric:0.5"})
add("o-uniform20", G, "koraki do 20", "A-runs, korak enakomerno 1–20.", {"layouts": every("runs"), "step": "uniform:20"})
add("o-uniform100", G, "koraki do 100", "A-runs, korak enakomerno 1–100.", {"layouts": every("runs"), "step": "uniform:100"})
add("o-bitflip-only", G, "samo preklop bita", "Binarna koda, mutacija samo preklop bita.", {"layouts": every("runs"), "width": 1024, "mutation": {"bitflip": 0.8, "insert_record": 0.1, "remove_record": 0.1}})
add("o-gray-mixed", G, "Gray: bit + ±1", "Grayeva koda, preklop bita in ±1 skupaj.", {"layouts": every("gray"), "width": 1024, "mutation": {"bitflip": 0.3, "creep": 0.3, "reset": 0.2, "insert_record": 0.1, "remove_record": 0.1}})
add("o-structural", G, "strukturno bogat", "Več dodajanja in odstranjevanja zapisov.", {"mutation": {"creep": 0.3, "reset": 0.2, "insert_record": 0.25, "remove_record": 0.25}})

# --- 5. Record shape and addressing ----------------------------------------------------
G = "oblika zapisa in naslavljanje"
add("s-sources1", G, "1 vir na vozel", "Vsak vozel ima en sam vir.", {"sources": 1})
add("s-sources3", G, "3 viri na vozel", "Do trije viri na vozel.", {"sources": 3})
add("s-sources3-runs", G, "3 viri, nizi", "Trije viri, mesta v nizih.", {"sources": 3, "layouts": every("runs")})
add("s-outsum", G, "izhod vsota", "Izhod sešteje dva vira.", {"output_sources": 2})
add("s-outsum-runs", G, "izhod vsota, nizi", "Izhod sešteje dva vira, nizi.", {"output_sources": 2, "layouts": every("runs")})
add("s-reach2", G, "doseg 2", "Relativni viri sežejo le 2 zapisa daleč.", {"reach": 2})
add("s-reach4", G, "doseg 4", "Doseg 4.", {"reach": 4})
add("s-reach16", G, "doseg 16", "Doseg 16.", {"reach": 16})
add("s-reach32", G, "doseg 32", "Doseg 32.", {"reach": 32})
add("s-absolute", G, "absolutno naslavljanje", "Vir je indeks vozla od začetka genoma.", {"addressing": "absolute"})
add("s-absolute-runs", G, "absolutno, nizi", "Absolutno naslavljanje, nizi.", {"addressing": "absolute", "layouts": every("runs")})
add("s-absolute-gray", G, "absolutno, Gray", "Absolutno naslavljanje, Grayeva koda.", {"addressing": "absolute", "layouts": every("gray"), "width": 1024})
add("s-init-small", G, "majhni začetni genomi", "Začetek z 1–3 vozli.", {"layouts": every("runs"), "init_nodes": [1, 3]})
add("s-init-large", G, "veliki začetni genomi", "Začetek z 10–20 vozli.", {"layouts": every("runs"), "init_nodes": [10, 20]})
add("s-init-huge", G, "zelo veliki začetni genomi", "Začetek z 20–30 vozli.", {"layouts": every("runs"), "init_nodes": [20, 30]})

# --- 6. Chimeras and transfer -------------------------------------------------------------
G = "himere in prenos genov"
add("c-transfer", G, "horizontalni prenos", "A-base, križanje vstavi en zapis drugega starša.", {}, ["--crossover", "transfer"])
add("c-transfer-runs", G, "horizontalni prenos, nizi", "A-runs s horizontalnim prenosom.", {"layouts": every("runs")}, ["--crossover", "transfer"])
add("c-chimera", G, "himera", "Kodon za tip, diploiden vir, log uteži, vezava po oznakah.", {"addressing": "tag", "layouts": {"kind": "codon", "source": "diploid:32", "parameter": "runs", "weight": "log"}, "mutation": {"base": 0.3, "creep": 0.3, "reset": 0.15, "insert_record": 0.1, "remove_record": 0.05, "duplicate_record": 0.05, "transpose": 0.05}})
add("c-all-nature", G, "vsa narava", "Vezava po oznakah, kodon, kanalizacija, log uteži, naravne mutacije, prenos genov.", {"addressing": "tag", "layouts": {"kind": "codon", "source": "runs", "parameter": "majority:10", "weight": "log", "tag": "runs"}, "mutation": NATURE_MIX}, ["--crossover", "transfer"])
add("c-chaos", G, "kaos", "Kaotična preslikava za vsako vlogo: logistična, sinus, Collatz, zlati rez.", {"layouts": {"kind": "logistic:3.99", "source": "sin:37", "parameter": "collatz", "weight": "golden"}})
add("c-smooth", G, "gladko", "Vse vloge v širokih nizih, fine uteži, geometrijski koraki.", {"layouts": {"kind": "runs", "source": "runs", "parameter": "runs", "weight": "fine"}, "width": 4096, "step": "geometric:0.7"})
add("c-diploid-tag", G, "diploidna vezava", "Vezava po oznakah, diploidne oznake in viri.", {"addressing": "tag", "layouts": {"kind": "diploid:32", "source": "diploid:32", "parameter": "runs", "weight": "log", "tag": "diploid:32"}})
add("c-integral-tag", G, "integral + vezava", "Vezava po oznakah, oznake kot tekoča vsota.", {"addressing": "tag", "layouts": {**every("runs"), "tag": "cumulative"}})
add("c-codon-burst", G, "kodon + sevanje", "Kodoni z izbruhi in podvojitvami.", {"layouts": every("codon"), "mutation": {"base": 0.4, "reset": 0.1, "insert_record": 0.1, "remove_record": 0.1, "burst": 0.15, "duplicate_record": 0.15}})
add("c-majority-gray", G, "kanalizacija + Gray tip", "Tip v Grayevi kodi, parametri in uteži z večino.", {"layouts": {"kind": "gray", "source": "runs", "parameter": "majority:10", "weight": "majority:10"}, "width": 1024})
add("c-epigenetic-diploid", G, "utišanje + diploidnost", "Epigenetski tip, diploidni viri in uteži.", {"layouts": {"kind": "epigenetic", "source": "diploid:32", "parameter": "runs", "weight": "diploid:32"}})
add("c-golden-log-outsum", G, "zlati rez + log + vsota", "Zlati rez za strukturo, log uteži, izhod vsota.", {"layouts": {"kind": "golden", "source": "golden", "parameter": "runs", "weight": "log"}, "output_sources": 2})

REFERENCES = [
    ("A-base", "reference", "ref · A-base", "Dosedanja fiksna vloga, neposredno.", ["--method", "A-base"]),
    ("A-runs", "reference", "ref · A-runs", "Dosedanja fiksna vloga v nizih: najboljša doslej.", ["--method", "A-runs"]),
    ("ref-context", "reference", "ref · kontekstna", "Prosto zaporedje, kontekstna izbira gena.", ["--method", "ref-context"]),
]


def validate() -> None:
    """Every config must build and decode, so a typo fails here, not on Saturday."""
    from random import Random

    from ameba_genome import decode
    from ameba_signal import SignalGraphPolicy

    grammar = Grammar()
    policy = SignalGraphPolicy()
    for key, _, _, _, config, args in CATALOG:
        encoding = FixedEncoding(grammar, config=FixedConfig.from_dict(json.loads(json.dumps(config))))
        rng = Random(1)
        for _ in range(10):
            genome = encoding.random_genome(rng)
            for _ in range(10):
                genome = encoding.mutate(genome, rng)
            name = args[1] if args[:1] == ["--crossover"] else "crossover"
            other = encoding.random_genome(rng)
            genome = getattr(encoding, name)(genome, other, rng)
            policy.validate(decode(encoding.translate(genome), grammar))


def main() -> int:
    keys = [item[0] for item in CATALOG]
    if len(keys) != len(set(keys)):
        raise SystemExit("duplicate keys in the catalog")
    validate()
    series = []
    for key, group, label, desc, config, args in CATALOG:
        series.append({
            "key": key, "group": group, "label": label, "desc": desc,
            "args": ["--method", "A-custom", "--encoding", json.dumps(config, separators=(",", ":")), *args],
        })
    for key, group, label, desc, args in REFERENCES:
        series.append({"key": key, "group": group, "label": label, "desc": desc, "args": args})
    spec = {
        "dir": "results/weekend-encodings",
        "benchmarks": ["narendra"],
        "seeds": 12,
        "common": ["--generations", "3000", "--population", "300", "--progress", "100"],
        "title": "Sto kodiranj",
        "eyebrow": "AMEBA · razvoj genoma · Narendra · vikend",
        "h1": "Sto načinov, kako zapisati graf v števila",
        "lede": (f"{len(CATALOG)} kodiranj fiksne vloge in {len(REFERENCES)} reference na Narendri (SISO, 150 korakov). "
                 "GA je za vse enak (generacijski, turnir 3, elita 2, križanje 0,5, nevtralni drift); "
                 "populacija 300, 3000 generacij, 12 semen. Kodiranja se razlikujejo v preslikavi števila v pomen, "
                 "v mutacijskih operatorjih, obliki zapisa in naslavljanju; ena skupina posnema mehanizme iz narave."),
        "cards_h2": "Katalog",
        "series": series,
    }
    out = ROOT / "experiments" / "weekend-encodings.json"
    if out.exists():  # keep findings written after a run
        previous = json.loads(out.read_text(encoding="utf-8"))
        if previous.get("findings"):
            spec["findings"] = previous["findings"]
    out.write_text(json.dumps(spec, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(CATALOG)} encodings + {len(REFERENCES)} references validated and written to {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
