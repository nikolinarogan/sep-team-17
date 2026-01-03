import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Vehicle } from '../models/vehicle.models';

@Injectable({
  providedIn: 'root'
})
export class VehicleService {
  private apiUrl = 'http://localhost:8080/vehicles';

  constructor(private http: HttpClient) {}

  getAllVehicles(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>(this.apiUrl);
  }

  getVehicleById(id: number): Observable<Vehicle> {
    return this.http.get<Vehicle>(`${this.apiUrl}/${id}`);
  }

  createVehicle(vehicleData: FormData): Observable<Vehicle> {
    return this.http.post<Vehicle>(`${this.apiUrl}/create`, vehicleData);
  }

  updateVehicle(id: number, vehicleData: FormData): Observable<Vehicle> {
    return this.http.put<Vehicle>(`${this.apiUrl}/${id}`, vehicleData);
  }

  deleteVehicle(id: number): Observable<string> {
    return this.http.delete<string>(`${this.apiUrl}/${id}`, { 
      responseType: 'text' as 'json'
    });
  }
}

