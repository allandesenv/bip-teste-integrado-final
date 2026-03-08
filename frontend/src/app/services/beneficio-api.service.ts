import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Beneficio, BeneficioPayload, TransferPayload } from '../models/beneficio';

@Injectable({
  providedIn: 'root'
})
export class BeneficioApiService {
  private readonly baseUrl = '/api/v1/beneficios';

  constructor(private readonly http: HttpClient) {}

  list(): Observable<Beneficio[]> {
    return this.http.get<Beneficio[]>(this.baseUrl);
  }

  create(payload: BeneficioPayload): Observable<Beneficio> {
    return this.http.post<Beneficio>(this.baseUrl, payload);
  }

  update(id: number, payload: BeneficioPayload): Observable<Beneficio> {
    return this.http.put<Beneficio>(`${this.baseUrl}/${id}`, payload);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  transfer(payload: TransferPayload): Observable<Beneficio[]> {
    return this.http.post<Beneficio[]>(`${this.baseUrl}/transferencias`, payload);
  }
}
