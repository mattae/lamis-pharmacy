import {Component, OnInit} from '@angular/core';
import {DDDOutlet, Devolve, Patient, RelatedCD4, RelatedClinic, RelatedPharmacy, RelatedViralLoad} from '../model/pharmacy.model';
import * as moment_ from 'moment';
import {Moment} from 'moment';
import {PharmacyService} from '../services/pharmacy.service';
import {CardViewItem, CardViewTextItemModel, NotificationService} from '@alfresco/adf-core';
import {AppLoaderService, entityCompare} from '@lamis/web-core';
import {ActivatedRoute} from '@angular/router';
import {DevolveService} from '../services/devolve.service';
import {Observable} from 'rxjs';
import {HttpErrorResponse, HttpResponse} from '@angular/common/http';

const moment = moment_;

export interface Dmoc {
    name: string;
    value: string;
}

@Component({
    selector: 'devolve-edit',
    templateUrl: './devolve.edit.component.html'
})
export class DevolveEditComponent implements OnInit {
    entity: Devolve = {};
    relatedClinic: RelatedClinic;
    relatedPharmacy: RelatedPharmacy;
    relatedCD4: RelatedCD4;
    relatedViralLoad: RelatedViralLoad;
    dddOutlets: DDDOutlet[];
    states: any[] = [];
    lgas: any[];
    dmocTypes: Dmoc[] = [];
    patient: Patient;
    dateRegistration: Moment;
    maxNextVisit: Moment;
    isSaving: boolean;
    error = false;
    tomorrow = moment().add(1, 'days');
    today = moment();
    minNextAppointment: Moment;
    editing = {};
    state: any;
    lga: any;
    devolveDates: Moment[] = [];
    enableDDDOutlet = false;
    enableRefillGroup = false;
    properties: Array<CardViewItem> = [];
    minDate: Moment;
    minDiscontinued: Moment;
    refillClubs: string[] = [];
    filteredRefillClubs: string[] = [];
    refillClub: string;
    dddOutletType: string;
    lgaId: number;
    outletRequired = false;

    constructor(private pharmacyService: PharmacyService,
                private devolveService: DevolveService,
                protected notification: NotificationService,
                private appLoaderService: AppLoaderService,
                protected activatedRoute: ActivatedRoute) {
    }

    createEntity(): Devolve {
        return <Devolve>{};
    }

    ngOnInit(): void {
        this.dmocTypes.push({
            name: 'Adolescent Refill Club',
            value: 'ARC'
        });
        this.dmocTypes.push({
            name: 'CARC',
            value: 'CARC'
        });
        this.dmocTypes.push({
            name: 'CPARP',
            value: 'CPARP'
        });
        this.dmocTypes.push({
            name: 'F-CARG',
            value: 'F_CARG'
        });
        this.dmocTypes.push({
            name: 'Fast Track',
            value: 'FAST_TRACK'
        });
        this.dmocTypes.push({
            name: 'S-CARG',
            value: 'S_CARG'
        });
        this.dmocTypes.push({
            name: 'Home Refill',
            value: 'HOME_DELIVERY'
        });
        this.dmocTypes.push({
            name: 'DARF',
            value: 'DARF'
        });

        this.devolveService.getStates().subscribe(res => {
            this.states = res;
        });

        this.activatedRoute.data.subscribe(({entity}) => {
            this.entity = !!entity && entity.body ? entity.body : entity;
            if (this.entity === undefined) {
                this.entity = this.createEntity();
            }
            const patientId = this.activatedRoute.snapshot.paramMap.get('patientId');
            this.pharmacyService.getPatient(patientId).subscribe((res) => {
                this.entity.patient = res;
                this.patient = res;
                this.dateRegistration = res.dateRegistration;
                this.entity.facility = res.facility;
                this.minNextAppointment = this.dateRegistration.add(15, 'days');
                /*this.devolveService.getDevolveDatesByPatient(res.id).subscribe((res) => {
                    this.devolveDates = res;
                });*/
                this.updateRelated();
            });

            if (this.entity.id) {
                const dmoc = this.entity.dmocType;
                if (dmoc === 'MMD') {
                    this.dmocTypes.push({
                        name: 'MMD',
                        value: 'MMD'
                    });
                } else if (dmoc === 'MMS') {
                    this.dmocTypes.push({
                        name: 'MMS',
                        value: 'MMS'
                    });
                }

                if (this.entity.dddOutlet) {
                    this.enableDDDOutlet = true;
                    this.dddOutletType = this.entity.dddOutlet.type;
                    this.dddOutletChanged(this.entity.dddOutlet);

                    this.devolveService.getStateByLga(this.entity.dddOutlet.lga.id).subscribe(res => {
                        this.state = res;
                        this.stateChanged(this.state.id);
                        this.lga = this.entity.dddOutlet.lga;
                        this.lgaChanged(this.lga.id);
                    });
                }

                if (this.entity.dateDiscontinued) {
                    this.minDate = this.entity.dateDiscontinued.clone().add(2, 'day');
                } else {
                    this.minDiscontinued = this.entity.dateDevolved.clone().add(1, 'day');
                    this.minDate = this.entity.dateDevolved.clone().add(2, 'day');
                }

                if (this.entity.extra) {
                    this.refillClub = this.entity.extra.refillClub;
                    this.enableRefillGroup = this.entity.dmocType.includes('CAR');
                }
            }
        });

        this.devolveService.getRefillClubs().subscribe(res => this.refillClubs = res);
    }

    dateDiscontinuedChanged() {
        if (this.entity.dateDiscontinued) {
            this.minDate = this.entity.dateDiscontinued.clone().add(1, 'day');
        }
    }

    change(input: string) {
        if (input) {
            this.filteredRefillClubs = this.refillClubs.filter(f => f.toLowerCase().includes(input.toLowerCase()));
        }
    }

    filterDates(date: Moment): boolean {
        let exists = false;

        this.devolveDates.forEach(d => {
            if (date.diff(d, 'days') === 0) {
                exists = true;
            }
        });
        return (this.entity.id && date.diff(this.entity.dateDevolved, 'days') === 0) || !exists;
    }

    stateChanged(id) {
        this.devolveService.getLgasByState(id).subscribe(res => this.lgas = res);
    }

    lgaChanged(id) {
        this.lgaId = id;
        if (this.entity.dddOutlet && id !== this.entity.dddOutlet.lga.id) {
            this.entity.dddOutlet = undefined;
        }
        this.devolveService.getDDDOutletsByTypeAndLga(this.dddOutletType, id).subscribe(res => this.dddOutlets = res);
    }

    dddOutletChanged(outlet: DDDOutlet) {
        this.properties = [];
        this.properties.push(new CardViewTextItemModel({
            key: 'add',
            label: 'Address',
            value: outlet.address
        }));
        this.properties.push(new CardViewTextItemModel({
            key: 'phone',
            label: 'Telephone Number',
            value: outlet.phone
        }));
        this.properties.push(new CardViewTextItemModel({
            key: 'email',
            label: 'Email',
            value: outlet.email
        }));
    }

    dmocChanged(dmocType: string) {
        switch (dmocType) {
            case 'CPARP':
                this.dddOutletType = 'CPARP';
                break;
            case 'CARC':
                this.dddOutletType = 'CARC';
                break;
            case 'FAST_TRACK':
                this.dddOutletType = 'Fast Track';
                break;
            case 'S_CARG':
                this.dddOutletType = 'S-CARG';
                break;
            case 'F_CARG':
                this.dddOutletType = 'F-CARG';
                break;
            case 'ARC':
                this.dddOutletType = 'ARC';
                break;
            case 'HOME_REFILL':
                this.dddOutletType = 'Home Delivery';
                break;
            default:
                this.dddOutletType = dmocType;
        }
        this.enableDDDOutlet = !!this.dddOutletType;
        this.lgaChanged(this.lgaId);
        this.outletRequired = ['CPARP', 'DARF'].includes(this.entity.dmocType);
    }

    dateDevolvedChanged(date: Moment) {
        this.minNextAppointment = date.clone().add(7, 'days');
        this.maxNextVisit = date.clone().add(3, 'months');
        this.updateRelated();
    }

    updateRelated() {
        this.entity.relatedViralLoad = null;
        this.entity.relatedClinic = null;
        this.entity.relatedCd4 = null;
        this.entity.relatedPharmacy = null;
        if (this.entity.dateDevolved) {
            this.devolveService.getRelatedClinic(this.entity.id || 0, this.patient.id, this.entity.dateDevolved).subscribe(res => {
                this.relatedClinic = res;
                this.entity.relatedClinic = {id: res.id};
            });
            this.devolveService.getRelatedPharmacy(this.entity.id || 0, this.patient.id, this.entity.dateDevolved).subscribe(res => {
                this.relatedPharmacy = res;
                this.entity.relatedPharmacy = {id: res.id};
            });
            this.devolveService.getRelatedCD4(this.entity.id || 0, this.patient.id, this.entity.dateDevolved).subscribe(res => {
                this.relatedCD4 = res;
                this.entity.relatedCd4 = {id: res.id};
            });
            this.devolveService.getRelatedViralLoad(this.entity.id || 0, this.patient.id, this.entity.dateDevolved).subscribe(res => {
                this.relatedViralLoad = res;
                this.entity.relatedViralLoad = {id: res.id};
            });
        }
    }

    entityCompare(e1, e2) {
        return entityCompare(e1, e2);
    }

    previousState() {
        window.history.back();
    }

    save() {
        // this.progressBar.mode = 'indeterminate';
        this.appLoaderService.open('Saving visit...');
        this.isSaving = true;
        if (!this.entity.extra) {
            this.entity.extra = {};
        }
        this.entity.extra.refillClub = this.refillClub;
        if (this.entity.id !== undefined) {
            this.subscribeToSaveResponse(this.devolveService.update(this.entity));
        } else {
            this.subscribeToSaveResponse(this.devolveService.create(this.entity));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<any>>) {
        result.subscribe(
            (res: HttpResponse<any>) => this.onSaveSuccess(res.body),
            (res: HttpErrorResponse) => {
                this.appLoaderService.close();
                this.onSaveError();
                this.onError(res.message);
            });
    }

    private onSaveSuccess(result: any) {
        this.appLoaderService.close();
        this.isSaving = false;
        this.notification.showInfo('Devolve successfully saved');
        this.previousState();
    }

    private onSaveError() {
        this.isSaving = false;
        this.error = true;
        this.notification.showError('Error saving devolve');
    }

    protected onError(errorMessage: string) {
        this.isSaving = false;
        this.notification.showError(errorMessage);
    }
}
