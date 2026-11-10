# Stack optimization

Picks provider and instance type deterministically from the catalog. The LLM only supplies requirements.

## Requirements

- **Always hard:** `vcpuMin`, `memoryGbMin`, `area` (EU, US or ANY), `maxMonthlyEur` (applied to total cost).
- **Cost input:** `estimatedEgressGbPerMonth`.
- **Workload traits** (optional): CPU profile, architecture, storage kind, minimum disk. Each trait is either:
  - `STATED`: the user said it, so it's a hard filter.
  - `INFERRED`: the LLM concluded it from the use case, so it's a soft preference that never excludes.

  A wrong inference therefore can't lead to "nothing fits".
- No architecture given → x86 only, because ARM would break most images.

## Ranking order

1. Drop entries failing size, area, budget or any `STATED` trait.
2. Prefer entries meeting the most `INFERRED` traits.
3. Then the lowest total monthly cost: instance + disk + egress beyond the included amount + public IPv4.

The draft shows the cost breakdown and which preferences were met or missed.

## Catalog

The only provider today is the local Docker "cloud" ([local-provider.md](local-provider.md)). Its instance types map
to real container settings (memory, pinned or shared CPU, image architecture, volume vs local disk); prices and
regions are simulated.

The trait model is built for real clouds, where the differences matter: AWS `t3` burstable credits vs dedicated
`c`/`m` families, Hetzner's included traffic vs AWS per-GB egress, local NVMe vs network disk. A real provider plugs
in as catalog entries.

## Classes

`Ranker` (filter and order) · `TraitMatcher` (trait fit and labels) · `CostCalculator` (total cost) · `PriceCatalog`

## Limitation

`PriceCatalog` is hardcoded and simulated. With a real provider it becomes a table refreshed from the provider's
pricing API, falling back to the last good snapshot.
