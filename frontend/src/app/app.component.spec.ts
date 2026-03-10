import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render heading', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Gestao de Beneficios');
  });

  it('should request list on init', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const request = httpMock.expectOne('/api/v1/beneficios');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('should not submit beneficio when form invalid', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/beneficios').flush([]);

    component.beneficioForm.patchValue({ nome: '', valor: -1 });
    component.submitBeneficio();

    httpMock.expectNone('/api/v1/beneficios');
    expect(component.beneficioForm.invalid).toBeTrue();
  });

  it('should create beneficio on valid form submit', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/beneficios').flush([]);

    component.beneficioForm.patchValue({
      nome: 'Vale',
      descricao: 'Alimentacao',
      valor: 100,
      ativo: true
    });
    component.submitBeneficio();

    const request = httpMock.expectOne('/api/v1/beneficios');
    expect(request.request.method).toBe('POST');
    request.flush({ id: 1, nome: 'Vale', descricao: 'Alimentacao', valor: 100, ativo: true, version: 0 });
    httpMock.expectOne('/api/v1/beneficios').flush([]);
  });

  it('should show error message when list fails', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const request = httpMock.expectOne('/api/v1/beneficios');
    request.flush({ message: 'Falha' }, { status: 500, statusText: 'Server Error' });

    expect(component.error).toBe('Falha');
  });

  it('should call transfer API when transfer form valid', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/beneficios').flush([]);

    component.transferenciaForm.patchValue({ fromId: 1, toId: 2, amount: 10 });
    component.submitTransferencia();

    const request = httpMock.expectOne('/api/v1/beneficios/transferencias');
    expect(request.request.method).toBe('POST');
    request.flush([]);
    httpMock.expectOne('/api/v1/beneficios').flush([]);
  });

  it('should not call transfer when form invalid', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/beneficios').flush([]);

    component.transferenciaForm.patchValue({ fromId: 0, toId: 2, amount: 10 });
    component.submitTransferencia();

    httpMock.expectNone('/api/v1/beneficios/transferencias');
  });

  it('should call delete endpoint', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/beneficios').flush([]);

    component.deleteBeneficio(10);

    const request = httpMock.expectOne('/api/v1/beneficios/10');
    expect(request.request.method).toBe('DELETE');
    request.flush({});
    httpMock.expectOne('/api/v1/beneficios').flush([]);
  });

  it('should surface transfer error message', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/beneficios').flush([]);

    component.transferenciaForm.patchValue({ fromId: 1, toId: 2, amount: 10 });
    component.submitTransferencia();

    const request = httpMock.expectOne('/api/v1/beneficios/transferencias');
    request.flush({ message: 'Saldo insuficiente' }, { status: 409, statusText: 'Conflict' });

    expect(component.error).toBe('Saldo insuficiente');
  });
});
