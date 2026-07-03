-- Migration: Schema Upgrade for Viaturas and Militares (V3 Addendum)
-- Created: 2026-06-30

-- 1. CREATE VIATURAS TABLE
CREATE TABLE IF NOT EXISTS viaturas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocorrencia_id UUID NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    prefixo VARCHAR(50) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    unidade VARCHAR(100),
    km_saida INTEGER,
    km_local INTEGER,
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. CREATE MILITARES TABLE
CREATE TABLE IF NOT EXISTS militares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    viatura_id UUID NOT NULL REFERENCES viaturas(id) ON DELETE CASCADE,
    re VARCHAR(20) NOT NULL,
    nome_guerra VARCHAR(100) NOT NULL,
    graduacao VARCHAR(50) NOT NULL,
    funcao VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. UPDATE VITIMAS TABLE WITH VIATURA SOCORRO AND DESTINO FIELDS
ALTER TABLE vitimas ADD COLUMN IF NOT EXISTS viatura_socorro_id UUID REFERENCES viaturas(id) ON DELETE SET NULL;
ALTER TABLE vitimas ADD COLUMN IF NOT EXISTS hospital_destino VARCHAR(255);
ALTER TABLE vitimas ADD COLUMN IF NOT EXISTS transportado_por VARCHAR(100);

-- 4. ENABLE ROW LEVEL SECURITY
ALTER TABLE viaturas ENABLE ROW LEVEL SECURITY;
ALTER TABLE militares ENABLE ROW LEVEL SECURITY;

-- 5. CREATE RLS POLICIES FOR VIATURAS
CREATE POLICY "Allow authenticated reads on viaturas" ON viaturas FOR SELECT TO authenticated USING (true);
CREATE POLICY "Allow authenticated inserts on viaturas" ON viaturas FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Allow authenticated updates on viaturas" ON viaturas FOR UPDATE TO authenticated USING (true);
CREATE POLICY "Allow authenticated deletes on viaturas" ON viaturas FOR DELETE TO authenticated USING (true);

-- 6. CREATE RLS POLICIES FOR MILITARES
CREATE POLICY "Allow authenticated reads on militares" ON militares FOR SELECT TO authenticated USING (true);
CREATE POLICY "Allow authenticated inserts on militares" ON militares FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Allow authenticated updates on militares" ON militares FOR UPDATE TO authenticated USING (true);
CREATE POLICY "Allow authenticated deletes on militares" ON militares FOR DELETE TO authenticated USING (true);

-- 7. TRIGGERS FOR updated_at ON VIATURAS AND MILITARES
CREATE TRIGGER update_viaturas_updated_at
    BEFORE UPDATE ON viaturas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_militares_updated_at
    BEFORE UPDATE ON militares
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
