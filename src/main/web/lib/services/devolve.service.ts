import {Inject, Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {DATE_FORMAT, SERVER_API_URL_CONFIG, ServerApiUrlConfig} from '@lamis/web-core';
import {map} from 'rxjs/operators';
import * as moment_ from 'moment';
import {Moment} from 'moment';
import {
    CommunityPharmacy,
    Devolve,
    RelatedCD4,
    RelatedClinic,
    RelatedPharmacy,
    RelatedViralLoad
} from '../model/pharmacy.model';

const moment = moment_;

type EntityResponseType = HttpResponse<Devolve>;

@Injectable({providedIn: 'root'})
export class DevolveService {
    public resourceUrl = '';

    constructor(protected http: HttpClient, @Inject(SERVER_API_URL_CONFIG) private serverUrl: ServerApiUrlConfig) {
        this.resourceUrl = serverUrl.SERVER_API_URL + '/api/devolves';
    }

    create(devolve: Devolve): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(devolve);
        return this.http
            .post<Devolve>(this.resourceUrl, copy, {observe: 'response'})
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    update(devolve: Devolve): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(devolve);
        return this.http
            .put<Devolve>(this.resourceUrl, copy, {observe: 'response'})
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http
            .get<Devolve>(`${this.resourceUrl}/${id}`, {observe: 'response'})
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    findByUuid(id: string): Observable<EntityResponseType> {
        return this.http
            .get<Devolve>(`${this.resourceUrl}/by-uuid/${id}`, {observe: 'response'})
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }


    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, {observe: 'response'});
    }

    getDevolveDatesByPatient(patientId: number) {
        return this.http.get<Moment[]>(`${this.resourceUrl}/patient/${patientId}/visit-dates`)
            .pipe(map((res) => {
                    res.forEach(d => moment(d));
                    return res;
                })
            );
    }

    getStates() {
        return this.http.get<any[]>('/api/states');
    }

    getLgasByState(id) {
        return this.http.get<any[]>(`/api/provinces/state/${id}`);
    }

    getCommunityPharmaciesByLga(id) {
        return this.http.get<CommunityPharmacy[]>(`${this.resourceUrl}/community-pharmacies/lga/${id}`);
    }

    getRelatedPharmacy(devolveId: number, patientId: number, date: Moment) {
        const d = date.format(DATE_FORMAT);
        return this.http.get<RelatedPharmacy>(`${this.resourceUrl}/${devolveId}/patient/${patientId}/related-pharmacy/at/${d}`)
            .pipe(map(res => {
                if (res.dateVisit) {
                    res.dateVisit = moment(res.dateVisit).format('DD MMM, YYYY');
                }
                return res;
            }));
    }

    getRelatedClinic(devolveId: number, patientId: number, date: Moment) {
        const d = date.format(DATE_FORMAT);
        return this.http.get<RelatedClinic>(`${this.resourceUrl}/${devolveId}/patient/${patientId}/related-clinic/at/${d}`)
            .pipe(map(res => {
                if (res.dateVisit) {
                    res.dateVisit = moment(res.dateVisit).format('DD MMM, YYYY');
                }
                return res;
            }));
    }

    getRelatedViralLoad(devolveId: number, patientId: number, date: Moment) {
        const d = date.format(DATE_FORMAT);
        return this.http.get<RelatedViralLoad>(`${this.resourceUrl}/${devolveId}/patient/${patientId}/related-viral-load/at/${d}`)
            .pipe(map(res => {
                if (res.dateResultReceived) {
                    res.dateResultReceived = moment(res.dateResultReceived).format('DD MMM, YYYY');
                }
                return res;
            }));
    }

    getRelatedCD4(devolveId: number, patientId: number, date: Moment) {
        const d = date.format(DATE_FORMAT);
        return this.http.get<RelatedCD4>(`${this.resourceUrl}/${devolveId}/patient/${patientId}/related-cd4/at/${d}`)
            .pipe(map(res => {
                if (res.dateResultReceived) {
                    res.dateResultReceived = moment(res.dateResultReceived).format('DD MMM, YYYY');
                }
                return res;
            }));
    }

    getStateByLga(id) {
        return this.http.get(`/api/provinces/${id}/state`);
    }

    protected convertDateFromClient(devolve: Devolve): Devolve {
        const copy: Devolve = Object.assign({}, devolve, {
            dateDevolved: devolve.dateDevolved != null && devolve.dateDevolved.isValid() ? devolve.dateDevolved.format(DATE_FORMAT) : null,
            dateNextClinic: devolve.dateNextClinic != null && devolve.dateNextClinic.isValid() ? devolve.dateNextClinic.format(DATE_FORMAT) : null,
            dateNextRefill: devolve.dateNextRefill != null && devolve.dateNextRefill.isValid() ? devolve.dateNextRefill.format(DATE_FORMAT) : null,
            dateDiscontinued: devolve.dateDiscontinued != null && devolve.dateDiscontinued.isValid() ? devolve.dateDiscontinued.format(DATE_FORMAT) : null,
            dateReturnedToFacility: devolve.dateReturnedToFacility != null && devolve.dateReturnedToFacility.isValid() ? devolve.dateReturnedToFacility.format(DATE_FORMAT) : null
        });
        return copy;
    }

    protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.dateReturnedToFacility = res.body.dateReturnedToFacility != null ? moment(res.body.dateReturnedToFacility) : null;
            res.body.dateNextRefill = res.body.dateNextRefill != null ? moment(res.body.dateNextRefill) : null;
            res.body.dateNextClinic = res.body.dateNextClinic != null ? moment(res.body.dateNextClinic) : null;
            res.body.dateDevolved = res.body.dateDevolved != null ? moment(res.body.dateDevolved) : null;
            res.body.dateDiscontinued = res.body.dateDiscontinued != null ? moment(res.body.dateDiscontinued) : null;
        }
        return res;
    }
}
