import {Component, OnInit, ViewChild} from '@angular/core';
import {Adr, Devolve, Drug, DrugDTO, Patient, Pharmacy, PharmacyLine, Regimen, RegimenType, StatusHistory} from '../model/pharmacy.model';
import {PharmacyService} from '../services/pharmacy.service';
import {NotificationService} from '@alfresco/adf-core';
import {ActivatedRoute} from '@angular/router';
import {MatButton, MatProgressBar} from '@angular/material';
import {HttpErrorResponse, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ColumnMode} from '@swimlane/ngx-datatable';
import * as moment_ from 'moment';
import {Moment} from 'moment';
import {AppLoaderService, DATE_FORMAT, entityCompare} from '@lamis/web-core';

const moment = moment_;

export const IPT_TYPE = {
    'START_INITIATION': 'Start Initiation',
    'START_REFILL': 'Start Refill',
    'FOLLOWUP_INITIATION': 'Followup Initiation',
    'FOLLOWUP_REFILL': 'Followup Refill'
};

export interface Ipt {
    type?: string;
    completed?: boolean;
    dateCompleted?: Moment;
}

@Component({
    selector: 'lamis-pharmacy-edit',
    templateUrl: './pharmacy-edit.component.html'
})
export class PharmacyEditComponent implements OnInit {
    @ViewChild(MatProgressBar, {static: true}) progressBar: MatProgressBar;
    @ViewChild(MatButton, {static: true}) submitButton: MatButton;
    entity: Pharmacy = {};
    patient: Patient;
    dateRegistration: Moment;
    maxNextVisit: Moment;
    regimenTypes: RegimenType[] = [];
    regimens: Regimen[] = [];
    selectedRegimens: Regimen[] = [];
    adrs: Adr[];
    isSaving: boolean;
    error = false;
    tomorrow = moment().add(1, 'days');
    today = moment();
    minNextAppointment: Moment;
    ColumnMode = ColumnMode;
    editing = {};
    rows: PharmacyLine[] = [];
    drugIds = new Set();
    visitDates: Moment[] = [];
    devolve: Devolve;
    dmocType: string;
    drugs: Drug[] = [];
    iptSelected = false;
    ipt: Ipt = {};
    deadStatus: StatusHistory;
    lastIptDate: Moment;
    minIptCompletion: Moment;
    uncompletedIpt: boolean;

    constructor(private pharmacyService: PharmacyService,
                protected notification: NotificationService,
                private appLoaderService: AppLoaderService,
                protected activatedRoute: ActivatedRoute) {
    }

    createEntity(): Pharmacy {
        return <Pharmacy>{};
    }

    ngOnInit(): void {
        this.isSaving = false;
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
                this.minNextAppointment = this.dateRegistration.clone().add(15, 'days');
                this.pharmacyService.getVisitDatesByPatient(res.id).subscribe((res1) => {
                    this.visitDates = res1;
                });

                this.pharmacyService.regimenTypes().subscribe(res1 => {
                    this.regimenTypes = res1.filter(t => {
                        return this.entity.patient.extra && this.entity.patient.extra.prep && this.entity.patient.extra.prep.registered ?
                            t.id === 30 : t.id !== 30;
                    });
                });

                this.pharmacyService.hasDeadStatus(patientId).subscribe(r => this.deadStatus = r.body);
            });

            if (this.entity.id) {
                this.updateMinDates(this.entity.dateVisit);
                if (this.entity.extra && this.entity.extra.ipt) {
                    this.ipt = Object.assign({}, this.entity.extra.ipt, {
                        dateCompleted: this.entity.extra.ipt.dateCompleted != null ? moment(this.entity.extra.ipt.dateCompleted) : null
                    });
                }

                this.rows = [...this.entity.lines.map(r => {
                    this.pharmacyService.getDrugsByRegimen(r.regimen_id).subscribe((res: DrugDTO[]) => {
                        r.description = res.find(d => d.regimenDrug.id === r.regimen_drug_id).drug.name;
                    });
                    r.morning = r.morning || 0;
                    r.afternoon = r.afternoon || 0;
                    r.evening = r.evening || 0;
                    this.iptSelected = r.regimen_type_id === 15;
                    r.quantity = ((r.morning || 0) + (r.afternoon || 0) + (r.evening || 0)) * r.duration;

                    this.pharmacyService.getRegimenById(r.regimen_id).subscribe(res => {
                        if (!this.regimens.map(rs => rs.id).includes(r.regimen_id)) {
                            this.regimens.push(res);
                            this.selectedRegimens.push(res);
                            this.regimens = [...this.regimens];
                            this.selectedRegimens = [...this.selectedRegimens];
                        }
                    });
                    return r;
                })];

                this.entity.duration = this.entity.lines.filter(r => {
                    return r.regimen_type_id === 1 || r.regimen_type_id === 2 || r.regimen_type_id === 3
                        || r.regimen_type_id === 4 || r.regimen_type_id === 14;
                }).map(r => r.duration)
                    .sort((r1, r2) => r1 - r2)
                    .pop();

                if (!this.entity.duration) {
                    this.entity.duration = this.entity.lines.map(r => r.duration)
                        .sort((r1, r2) => r1 - r2)
                        .pop();
                }

                this.pharmacyService.getDevolvement(this.entity.patient.id, this.entity.dateVisit).subscribe(res => {
                    this.devolve = res;
                    this.updateDmocType();
                });
            }
        });
    }

    dateVisitChanged(date: Moment) {
        this.entity.nextAppointment = this.suggestedNextAppointment();
        this.updateMinDates(date);
    }

    updateMinDates(date: Moment) {
        this.minNextAppointment = this.entity.nextAppointment.clone().subtract(7, 'days');
        this.maxNextVisit = this.entity.nextAppointment.clone().add(7, 'months');
        this.pharmacyService.getDevolvement(this.entity.patient.id, this.entity.dateVisit).subscribe(res => {
            this.devolve = res;
            this.updateDmocType();
        });

        this.pharmacyService.dateOfLastIptBefore(this.entity.patient.id, date).subscribe(res => {
            this.lastIptDate = res;
            this.minIptCompletion = this.lastIptDate.clone().add(15, 'days');
            this.pharmacyService.hasUncompletedIptAfter(this.entity.patient.id, res).subscribe(r => {
                this.uncompletedIpt = r && this.lastIptDate.add(6, 'months').isBefore(date);
            });
        });
    }

    suggestedNextAppointment(): Moment {
        if (this.entity.dateVisit) {
            let nextAppointment = this.entity.dateVisit.clone().add(this.entity.duration - 2 || 13, 'days');
            const weekday = nextAppointment.isoWeekday();
            if (weekday === 6) {
                nextAppointment = nextAppointment.clone().add(2, 'days');
            } else if (weekday === 7) {
                nextAppointment = nextAppointment.clone().add(1, 'days');
            }
            return nextAppointment;
        }
        return null;
    }

    updateDmocType() {
        let type = 'MMD';
        switch (this.devolve.dmocType) {
            case 'ARC':
                type = 'Adolescent Refill Club';
                break;
            case 'CPARP':
                type = 'CPARP';
                break;
            case 'CARC':
                type = 'CARC';
                break;
            case 'F_CARG':
                type = 'F-CARG';
                break;
            case 'FAST_TRACK':
                type = 'Fast Track';
                break;
            case 'S_CARG':
                type = 'S-CARG';
                break;
            case 'MMS':
                type = 'MMS';
                break;
            case 'HOME_REFILL':
                type = 'Home Refill';
                break;
        }
        this.dmocType = type;
    }

    filterDates(date: Moment): boolean {
        let exists = false;

        this.visitDates.forEach(d => {
            if (date.diff(d, 'days') === 0) {
                exists = true;
            }
        });
        return (this.entity.id && date.diff(this.entity.dateVisit, 'days') === 0) || !exists;
    }


    previousState() {
        window.history.back();
    }

    entityCompare(e1, e2) {
        return entityCompare(e1, e2);
    }

    save() {
        this.submitButton.disabled = true;
        // this.progressBar.mode = 'indeterminate';
        this.appLoaderService.open('Saving visit...');
        this.entity.lines = this.rows;
        this.isSaving = true;
        if (!this.entity.extra) {
            this.entity.extra = {};
        }
        if (!!this.ipt) {
            if (this.ipt.type) {
                this.ipt.dateCompleted = null;
            }
            this.entity.extra.ipt = Object.assign({}, this.ipt, {
                dateCompleted: this.ipt.dateCompleted != null && this.ipt.dateCompleted.isValid() ?
                    this.ipt.dateCompleted.format(DATE_FORMAT) : null
            });
        }

        if (this.deadStatus && this.deadStatus.dateStatus) {
            if (this.deadStatus.dateStatus.isBefore(this.entity.dateVisit)) {
                this.notification.showError(`Cannot save refill, patient was declared dead
                 (${this.deadStatus.dateStatus.format('DD MMMM, YYYY')}) before date of current refill`);
                this.appLoaderService.close();
                this.isSaving = false;
                return;
            }
        }

        if (this.entity.id !== undefined) {
            this.subscribeToSaveResponse(this.pharmacyService.update(this.entity));
        } else {
            this.subscribeToSaveResponse(this.pharmacyService.create(this.entity));
        }
    }

    regimenTypeChange(type: any) {
        this.pharmacyService.regimesByRegimenType(type.id).subscribe((res: Regimen[]) => {
            res.forEach((regimen: Regimen) => {
                if (!this.regimens.map(r => r.id).includes(regimen.id)) {
                    this.regimens.push(regimen);
                    this.regimens = [...this.regimens];
                }
            });
        });
    }

    durationChanged(duration) {
        this.rows = this.rows.map(r => {
            r.duration = duration;
            r.quantity = (r.morning + r.afternoon + r.evening) * duration;
            return r;
        });
        this.rows = [...this.rows];

        this.entity.nextAppointment = this.suggestedNextAppointment();

        if (duration === 90) {
            this.entity.mmdType = 'MMD-3';
        } else if (duration === 120) {
            this.entity.mmdType = 'MMD-4';
        } else if (duration === 150) {
            this.entity.mmdType = 'MMD-5';
        } else if (duration === 180) {
            this.entity.mmdType = 'MMD-6';
        } else {
            this.entity.mmdType = null;
        }
    }

    iptTypes() {
        return IPT_TYPE;
    }

    regimenChange(event) {
        this.selectedRegimens.forEach(regimen => {
            this.iptSelected = regimen.regimenType.id === 15;
            this.pharmacyService.getDrugsByRegimen(regimen.id).subscribe((res: DrugDTO[]) => {
                res.forEach((drug: DrugDTO) => {
                    if (!this.rows.map(r => r.description).includes(drug.drug.name)) {
                        this.rows.push({
                            drug: drug.drug,
                            description: drug.drug.name,
                            morning: drug.drug.morning,
                            afternoon: drug.drug.afternoon,
                            evening: drug.drug.evening,
                            regimen_id: regimen.id,
                            duration: this.entity.duration,
                            quantity: this.entity.duration * ((drug.drug.morning || 0) + (drug.drug.afternoon || 0)
                                + (drug.drug.evening || 0)),
                            regimen_type_id: regimen.regimenType.id,
                            regimen_drug_id: drug.regimenDrug.id
                        });
                        this.rows = [...this.rows];
                        // this.drugs.push(drug.drug);
                    }

                    this.rows = this.rows.filter(row => this.selectedRegimens.map(regimen1 => regimen1.id).includes(row.regimen_id));
                    this.drugs.forEach(drug1 => {
                        if (!this.rows.map(r => r.description).includes(drug1.name)) {
                            this.drugs = this.drugs.filter(d => d.id !== drug1.id);
                        }
                    });
                });
            });
        });
    }

    updateValue(event, cell, rowIndex) {
        this.editing[rowIndex + '-' + cell] = false;
        this.rows[rowIndex][cell] = event.target.value;
        if (this.entity.duration) {
            const total = parseInt(this.rows[rowIndex]['morning'] + '' || '0', 10) +
                parseInt(this.rows[rowIndex]['afternoon'] + '' || '0', 10) +
                parseInt(this.rows[rowIndex]['evening'] + '' || '0', 10);
            this.rows[rowIndex]['quantity'] = (total * this.entity.duration);
        }
        this.rows = [...this.rows];
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
        this.notification.showInfo('Pharmacy visit successfully saved');
        this.previousState();
    }

    private onSaveError() {
        this.isSaving = false;
        this.error = true;
        this.notification.showError('Error saving pharmacy visit');
    }

    protected onError(errorMessage: string) {
        this.isSaving = false;
        this.notification.showError(errorMessage);
    }
}
