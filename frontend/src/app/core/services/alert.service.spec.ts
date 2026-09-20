import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AlertService } from './alert.service';
import { Alert } from '../models/api.models';

describe('AlertService', () => {
  let service: AlertService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AlertService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AlertService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads active alerts from the patient endpoint', () => {
    const response = { content: [] as Alert[], totalElements: 0, totalPages: 0, number: 0, size: 50 };

    service.active('patient-1').subscribe(result => expect(result).toEqual(response));

    const request = http.expectOne(request => request.url.endsWith('/alerts/patient-1/active'));
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });

  it('posts lifecycle actions to the alert endpoint', () => {
    const response = {} as Alert;

    service.acknowledge('alert-1').subscribe(result => expect(result).toEqual(response));
    const acknowledge = http.expectOne(request => request.url.endsWith('/alerts/by-id/alert-1/acknowledge'));
    expect(acknowledge.request.method).toBe('POST');
    acknowledge.flush(response);

    service.resolve('alert-1').subscribe(result => expect(result).toEqual(response));
    const resolve = http.expectOne(request => request.url.endsWith('/alerts/by-id/alert-1/resolve'));
    expect(resolve.request.method).toBe('POST');
    resolve.flush(response);
  });
});
