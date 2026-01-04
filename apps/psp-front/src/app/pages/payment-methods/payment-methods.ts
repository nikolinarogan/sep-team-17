import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentMethod } from '../../services/payment-method';
@Component({
  selector: 'app-payment-methods',
  imports: [CommonModule, FormsModule],
  templateUrl: './payment-methods.html',
  styleUrl: './payment-methods.css',
})
export class PaymentMethods implements OnInit {
  methods: any[] = [];
  newMethod = { name: '', serviceUrl: '' };

  constructor(private pmService: PaymentMethod) {}

  ngOnInit() {
    this.loadMethods();
  }

  loadMethods() {
    this.pmService.getAllMethods().subscribe(data => this.methods = data);
  }

  addMethod() {
    this.pmService.createMethod(this.newMethod).subscribe({
      next: (res) => {
        this.methods.push(res);
        this.newMethod = { name: '', serviceUrl: '' };
      },
      error: (err) => alert("Metoda već postoji ili je URL neispravan")
    });
  }

  deleteMethod(id: number) {
    if(confirm("Da li ste sigurni? Ovo može uticati na prodavce koji koriste ovaj metod.")) {
      this.pmService.deleteMethod(id).subscribe(() => {
        this.methods = this.methods.filter(m => m.id !== id);
      });
    }
  }
}
