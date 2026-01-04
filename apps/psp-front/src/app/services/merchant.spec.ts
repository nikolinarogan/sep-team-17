import { TestBed } from '@angular/core/testing';

import { Merchant } from './merchant';

describe('Merchant', () => {
  let service: Merchant;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Merchant);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
