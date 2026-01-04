import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MerchantDetails } from './merchant-details';

describe('MerchantDetails', () => {
  let component: MerchantDetails;
  let fixture: ComponentFixture<MerchantDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MerchantDetails]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MerchantDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
