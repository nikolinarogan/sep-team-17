import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CryptoCheckout } from './crypto-checkout';

describe('CryptoCheckout', () => {
  let component: CryptoCheckout;
  let fixture: ComponentFixture<CryptoCheckout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CryptoCheckout]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CryptoCheckout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
