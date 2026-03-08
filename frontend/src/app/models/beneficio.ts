export interface Beneficio {
  id: number;
  nome: string;
  descricao: string | null;
  valor: number;
  ativo: boolean;
  version: number;
}

export interface BeneficioPayload {
  nome: string;
  descricao: string;
  valor: number;
  ativo: boolean;
}

export interface TransferPayload {
  fromId: number;
  toId: number;
  amount: number;
}
