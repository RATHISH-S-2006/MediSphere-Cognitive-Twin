import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RiskService } from './risk.service';

describe('RiskService', () => {
  let service: RiskService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [RiskService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RiskService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads typed risk history through the Spring Boot API', () => {
    service.history('patient-1').subscribe();

    const request = http.expectOne('http://localhost:8082/api/risk/patient-1?page=0&size=10');
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 });
  });
});