#!/usr/bin/env python3
"""Resume un resultados.jtl de JMeter: cantidad, tiempos (promedio y percentiles) y codigos por paso."""
import csv
import statistics as st
import sys
from collections import defaultdict

tiempos, codigos, errores = defaultdict(list), defaultdict(lambda: defaultdict(int)), defaultdict(int)
inicio, fin = None, None
for r in csv.DictReader(open(sys.argv[1])):
    t = int(r['timeStamp'])
    inicio = t if inicio is None else min(inicio, t)
    fin = t + int(r['elapsed']) if fin is None else max(fin, t + int(r['elapsed']))
    tiempos[r['label']].append(int(r['elapsed']))
    codigos[r['label']][r['responseCode']] += 1
    if r['success'] != 'true':
        errores[r['label']] += 1


def pct(v, q):
    v = sorted(v)
    return v[min(len(v) - 1, int(q * len(v)))]


duracion = (fin - inicio) / 1000
total = sum(len(v) for v in tiempos.values())
print(f"Duracion: {duracion:.0f} s - {total} requests - {total / duracion:.1f} req/s - errores: {sum(errores.values())}")
print("| Paso | Requests | Prom. (ms) | p50 | p90 | p95 | p99 | Máx. | Errores | Códigos |")
print("|---|---|---|---|---|---|---|---|---|---|")
for paso in sorted(tiempos):
    v = tiempos[paso]
    cods = ", ".join(f"{c}: {n}" for c, n in sorted(codigos[paso].items()))
    print(f"| {paso} | {len(v)} | {st.mean(v):.0f} | {pct(v, .5)} | {pct(v, .9)} | {pct(v, .95)} | {pct(v, .99)} | "
          f"{max(v)} | {errores[paso]} | {cods} |")
