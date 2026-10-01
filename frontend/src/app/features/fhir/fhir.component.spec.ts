import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FhirComponent } from './fhir.component';

describe('FhirComponent', () => {
  it('creates the clinical data page', async () => {
    await TestBed.configureTestingModule({ imports: [FhirComponent], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    expect(TestBed.createComponent(FhirComponent).componentInstance).toBeTruthy();
  });
});
