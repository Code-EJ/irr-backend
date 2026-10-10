# Synthetic fiscal-document fixture

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

`verification.pdf` is the same valid one-page synthetic PDF maintained in the frontend contract fixtures. It is not a real tax invoice or personal document. SHA-256: `1716a8a315fe809bb0e4595dfcf0fd6c59358bcd75fbec3a455ac5145b058410`.

The benchmark copies these bytes into its owned disposable attachment directory under deterministic canonical identifiers. Corresponding rows are scoped to the two synthetic organizations. This preserves usable file provenance for sale references; the read-report workload does not benchmark downloads or fiscal validation. Never replace retained development invoices with this fixture.
