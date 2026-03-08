import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Beneficio, BeneficioPayload, TransferPayload } from './models/beneficio';
import { BeneficioApiService } from './services/beneficio-api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  beneficios: Beneficio[] = [];
  editingId: number | null = null;
  loading = false;
  feedback = '';
  error = '';

  beneficioForm = this.fb.group({
    nome: ['', [Validators.required]],
    descricao: [''],
    valor: [0, [Validators.required, Validators.min(0)]],
    ativo: [true, [Validators.required]]
  });

  transferenciaForm = this.fb.group({
    fromId: [0, [Validators.required, Validators.min(1)]],
    toId: [0, [Validators.required, Validators.min(1)]],
    amount: [0, [Validators.required, Validators.min(0.01)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: BeneficioApiService
  ) {}

  ngOnInit(): void {
    this.loadBeneficios();
  }

  loadBeneficios(): void {
    this.loading = true;
    this.api.list().subscribe({
      next: (data) => {
        this.beneficios = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = this.extractError(err);
        this.loading = false;
      }
    });
  }

  submitBeneficio(): void {
    if (this.beneficioForm.invalid) {
      this.beneficioForm.markAllAsTouched();
      return;
    }

    const payload: BeneficioPayload = this.beneficioForm.getRawValue() as BeneficioPayload;
    const action$ = this.editingId
      ? this.api.update(this.editingId, payload)
      : this.api.create(payload);

    action$.subscribe({
      next: () => {
        this.feedback = this.editingId ? 'Beneficio atualizado com sucesso.' : 'Beneficio criado com sucesso.';
        this.error = '';
        this.cancelEdit();
        this.loadBeneficios();
      },
      error: (err) => {
        this.error = this.extractError(err);
      }
    });
  }

  startEdit(item: Beneficio): void {
    this.editingId = item.id;
    this.beneficioForm.patchValue({
      nome: item.nome,
      descricao: item.descricao ?? '',
      valor: item.valor,
      ativo: item.ativo
    });
  }

  cancelEdit(): void {
    this.editingId = null;
    this.beneficioForm.reset({ nome: '', descricao: '', valor: 0, ativo: true });
  }

  deleteBeneficio(id: number): void {
    this.api.remove(id).subscribe({
      next: () => {
        this.feedback = 'Beneficio removido com sucesso.';
        this.error = '';
        this.loadBeneficios();
      },
      error: (err) => {
        this.error = this.extractError(err);
      }
    });
  }

  submitTransferencia(): void {
    if (this.transferenciaForm.invalid) {
      this.transferenciaForm.markAllAsTouched();
      return;
    }

    const payload: TransferPayload = this.transferenciaForm.getRawValue() as TransferPayload;

    this.api.transfer(payload).subscribe({
      next: () => {
        this.feedback = 'Transferencia realizada com sucesso.';
        this.error = '';
        this.transferenciaForm.reset({ fromId: 0, toId: 0, amount: 0 });
        this.loadBeneficios();
      },
      error: (err) => {
        this.error = this.extractError(err);
      }
    });
  }

  coerceNonNegative(controlName: 'fromId' | 'toId' | 'amount'): void {
    const control = this.transferenciaForm.controls[controlName];
    const numericValue = Number(control.value);

    if (!Number.isNaN(numericValue) && numericValue < 0) {
      control.setValue(0);
    }
  }

  private extractError(err: any): string {
    this.feedback = '';
    return err?.error?.message || 'Falha ao processar requisicao.';
  }
}
