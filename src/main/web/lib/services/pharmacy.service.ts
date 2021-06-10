import {Inject, Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {DATE_FORMAT, SERVER_API_URL_CONFIG, ServerApiUrlConfig} from '@lamis/web-core';
import {map} from 'rxjs/operators';
import {
    Adr,
    Devolve,
    DrugDTO,
    Patient,
    Pharmacy,
    PharmacyLine,
    Regimen,
    RegimenInfo,
    RegimenType,
    StatusHistory
} from '../model/pharmacy.model';
import * as moment_ from 'moment';
import {Moment} from 'moment';

const moment = moment_;

type EntityResponseType = HttpResponse<Pharmacy>;
type EntityArrayResponseType = HttpResponse<Pharmacy[]>;

@Injectable({providedIn: 'root'})
export class PharmacyService {
    public resourceUrl = '';

    constructor(protected http: HttpClient, @Inject(SERVER_API_URL_CONFIG) private serverUrl: ServerApiUrlConfig) {
        this.resourceUrl = serverUrl.SERVER_API_URL + '/api/pharmacies';
    }

    create(pharmacy: Pharmacy): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(pharmacy);
        return this.http
            .post<Pharmacy>(this.resourceUrl, copy, {observe: 'response'})
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    update(pharmacy: Pharmacy): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(pharmacy);
        console.log('Lines', copy);
        return this.http
            .put<Pharmacy>(this.resourceUrl, copy, {observe: 'response'})
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http
            .get<Pharmacy>(`${this.resourceUrl}/${id}`, {observe: 'response'})
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    findByUuid(id: string): Observable<EntityResponseType> {
        return this.http
            .get<Pharmacy>(`${this.resourceUrl}/by-uuid/${id}`, {observe: 'response'})
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }


    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, {observe: 'response'});
    }

    getPatient(id: any) {
        return this.http.get<Patient>(`/api/patients/by-uuid/${id}`, {observe: 'body'})
            .pipe(map((res) => {
                if (res) {
                    res.dateRegistration = res.dateRegistration != null ? moment(res.dateRegistration) : null;
                }
                return res;
            }));
    }

    getVisitDatesByPatient(patientId: number) {
        return this.http.get<Moment[]>(`${this.resourceUrl}/patient/${patientId}/visit-dates`)
            .pipe(map((res) => {
                    res.forEach(d => moment(d));
                    return res;
                })
            );
    }

    dateOfLastIptBefore(patientId: number, date: Moment) {
        return this.http.get<Moment>(`${this.resourceUrl}/patient/${patientId}/last-ipt-at/${date.format(DATE_FORMAT)}`)
            .pipe(map(res => res != null ? moment(res) : null));
    }

    saveTptCompletion(patientId: number, dateOfCompletion: Moment) {
        return this.http.get(`${this.resourceUrl}/patient/${patientId}/complete-tpt/${dateOfCompletion.format(DATE_FORMAT)}`);
    }

    hasUncompletedIptAfter(patientId: number, date: Moment) {
        return this.http.get<Moment>(`${this.resourceUrl}/patient/${patientId}/uncompleted-ipt-after/${date.format(DATE_FORMAT)}`);
    }

    hasDeadStatus(puuid: string) {
        return this.http.get<StatusHistory>(`${this.resourceUrl}/patient/${puuid}/has-dead-status`, {observe: 'response'})
            .pipe(map(res => {
                if (res.body) {
                    res.body.dateStatus = res.body.dateStatus != null ? moment(res.body.dateStatus) : null;
                }
                return res;
            }));
    }

    regimenTypes() {
        return this.http.get<RegimenType[]>(`${this.resourceUrl}/regimen-types`);
    }

    regimenInfo(patientId: number) {
        return this.http.get<RegimenInfo>(`${this.resourceUrl}/regimen-info/patient/${patientId}`);
    }

    adrs() {
        return this.http.get<Adr[]>(`${this.resourceUrl}/adrs`);
    }

    getLinesByPharmacy(pharmacyId: number) {
        return this.http.get<PharmacyLine[]>(`${this.resourceUrl}/${pharmacyId}/lines`);
    }

    regimesByRegimenType(id: number) {
        return this.http.get<Regimen[]>(`${this.resourceUrl}/regimens/regimen-type/${id}`);
    }

    getDrugsByRegimen(id: number) {
        return this.http.get<DrugDTO[]>(`${this.resourceUrl}/drugs/regimen/${id}`);
    }

    getRegimenById(id) {
        return this.http.get<Regimen>(`${this.resourceUrl}/regimen/${id}`);
    }

    latestVisit(patientId: number) {
        return this.http.get<Pharmacy>(`${this.resourceUrl}/patient/${patientId}/latest`);
    }

    getDevolvement(patientId: number, date: Moment) {
        const d = date.format(DATE_FORMAT);
        return this.http.get<Devolve>(`${this.resourceUrl}/patient/${patientId}/devolvement/at/${d}`)
            .pipe(map(res => {
                res.dateDevolved = res.dateDevolved != null ? moment(res.dateDevolved) : null;
                res.dateReturnedToFacility = res.dateReturnedToFacility != null ? moment(res.dateReturnedToFacility) : null;
                res.dateNextClinic = res.dateNextClinic != null ? moment(res.dateNextClinic) : null;
                res.dateNextRefill = res.dateNextRefill != null ? moment(res.dateNextRefill) : null;
                return res;
            }));
    }

    protected convertDateFromClient(pharmacy: Pharmacy): Pharmacy {
        const copy: Pharmacy = Object.assign({}, pharmacy, {
            dateVisit: pharmacy.dateVisit != null && pharmacy.dateVisit.isValid() ? pharmacy.dateVisit.format(DATE_FORMAT) : null,
            nextAppointment: pharmacy.nextAppointment != null && pharmacy.nextAppointment.isValid() ?
                pharmacy.nextAppointment.format(DATE_FORMAT) : null
        });
        return copy;
    }

    protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.nextAppointment = res.body.nextAppointment != null ? moment(res.body.nextAppointment) : null;
            res.body.dateVisit = res.body.dateVisit != null ? moment(res.body.dateVisit) : null;
        }
        return res;
    }

    protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((pharmacy: Pharmacy) => {
                pharmacy.dateVisit = pharmacy.dateVisit != null ? moment(pharmacy.dateVisit) : null;
                pharmacy.nextAppointment = pharmacy.nextAppointment != null ? moment(pharmacy.nextAppointment) : null;
            });
        }
        return res;
    }
}
