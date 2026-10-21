# Evaluating the AI recommendations against recruiters' judgement

The platform asks a language model (Gemini) to pick, for a job seeker, the
five offers that fit them best and to justify each pick. This experiment
measures how far those picks agree with what experienced recruiters would
recommend, and whether the justifications are accurate.

## Design

| | |
|---|---|
| Candidates | 10 fictional profiles (`profiles.json`), one per sector represented in the offers. Fictional, so no real person's data is shown to anyone. Each has exactly the fields the platform sends to the model. |
| Offers | The 20 active offers of approved companies in the development database. Three test entries whose descriptions are 11–15 characters long are excluded. |
| Pools | Each candidate gets 10 offers: up to 5 from their own sector, the rest drawn at random (seed 20260917). Sector membership only builds the pool; it does not decide relevance. |
| Model | The platform's own prompt, model list, temperature and token limit, read from the backend sources. Each candidate is run 3 times. Explanations are requested in French. |
| Recruiters | 2–3 recruiters rate every pair 0–3 **without seeing the model's choices** (form A, ~40 min). Only afterwards do they judge the model's explanations (form B, ~15 min). |
| Baselines | *Keyword*: shared words between the candidate's skills/target role and the offer, plus region and language fit. *Random*: the expectation of five offers chosen at random. |
| Metrics | Relevance = mean recruiter grade ≥ 2. Precision of picks, P@5, recall@5, nDCG@5; weighted kappa between recruiters; exact Wilcoxon test (Gemini vs baselines, per candidate); stability (Jaccard across runs); share of accurate explanations. |

The model and the recruiters see the same information. Recruiters see it in
French (region and sector names translated); the model sees the platform's
English field names.

## Running it

From this folder, with the backend's `.env` in place and the local database running:

```bash
python build_evaluation_set.py      # data/evaluation_set.json
python run_gemini.py                # data/gemini_runs.json (resumable; --force to redo)
python make_forms.py R1 R2 R3       # forms/Evaluation_A_R*.xlsx and Evaluation_B_R*.xlsx
python analyse.py --self-test       # checks the analysis on SIMULATED grades; not a result
```

Send each recruiter **only** `Evaluation_A_<R>.xlsx`. When it comes back, put it
in `responses/` and send `Evaluation_B_<R>.xlsx`. Then:

```bash
python analyse.py                   # results/results.md and results/results_table.tex
```

## Limits to state with the results

- Ten candidates and twenty offers: enough to show a clear difference, not to
  measure small ones. Report the per-candidate table, not only the means.
- The offers are demonstration data with short descriptions; real adverts
  carry more information, for recruiters and model alike.
- The candidates are fictional. Real seekers' profiles are messier.
- Recruiters' grades are the reference, not the truth; their agreement
  (kappa) says how much weight that reference can bear.
