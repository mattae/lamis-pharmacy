import {Injectable} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot, Routes} from '@angular/router';
import {Observable, of} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {PharmacyService} from './pharmacy.service';
import {Pharmacy} from '../model/pharmacy.model';
import {PharmacyDetailsComponent} from '../components/pharmacy-details.component';
import {PharmacyEditComponent} from '../components/pharmacy-edit.component';

@Injectable()
export class PharmacyResolve implements Resolve<Pharmacy> {
    constructor(private service: PharmacyService) {
    }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Pharmacy> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.findByUuid(id).pipe(
                filter((response: HttpResponse<Pharmacy>) => response.ok),
                map((patient: HttpResponse<Pharmacy>) => patient.body)
            );
        }
        return of(<Pharmacy>{});
    }
}

export const ROUTES: Routes = [
    {
        path: '',
        data: {
            title: 'Pharmacy Refill',
            breadcrumb: 'PHARMACY REFILL'
        },
        children: [
            {
                path: ':id/patient/:patientId/view',
                component: PharmacyDetailsComponent,
                resolve: {
                    entity: PharmacyResolve
                },
                data: {
                    authorities: ['ROLE_USER'],
                    title: 'Pharmacy Refill',
                    breadcrumb: 'PHARMACY REFILL'
                },
                //canActivate: [UserRouteAccessService]
            },
            {
                path: 'patient/:patientId/new',
                component: PharmacyEditComponent,
                data: {
                    authorities: ['ROLE_DEC'],
                    title: 'Pharmacy Refill',
                    breadcrumb: 'ADD PHARMACY REFILL'
                },
                //canActivate: [UserRouteAccessService]
            },
            {
                path: ':id/patient/:patientId/edit',
                component: PharmacyEditComponent,
                resolve: {
                    entity: PharmacyResolve
                },
                data: {
                    authorities: ['ROLE_DEC'],
                    title: 'Pharmacy Refill Edit',
                    breadcrumb: 'PHARMACY REFILL EDIT'
                },
                //canActivate: [UserRouteAccessService]
            }
        ]
    }
];

