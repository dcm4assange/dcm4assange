package org.dcm4assange.net;

import org.dcm4assange.DicomObject;
import org.dcm4assange.Tag;
import org.dcm4assange.VR;
import org.dcm4assange.util.UIDUtils;

/**
 * @author Gunter Zeilinger (gunterze@protonmail.com)
 * @since Nov 2019
 */
public enum Dimse {
    C_STORE_RSP (0x8001, Tag.AffectedSOPClassUID, Tag.AffectedSOPInstanceUID, Tag.MessageIDBeingRespondedTo,
            0, null),
    C_STORE_RQ (0x0001, Tag.AffectedSOPClassUID, Tag.AffectedSOPInstanceUID, Tag.MessageID,
            Status.SOPclassNotSupported, C_STORE_RSP),
    C_GET_RSP (0x8010, Tag.AffectedSOPClassUID, 0, Tag.MessageIDBeingRespondedTo,
            0, null),
    C_GET_RQ (0x0010, Tag.AffectedSOPClassUID, 0, Tag.MessageID,
            Status.SOPclassNotSupported, C_GET_RSP),
    C_FIND_RSP (0x8020, Tag.AffectedSOPClassUID, 0, Tag.MessageIDBeingRespondedTo,
            0, null),
    C_FIND_RQ (0x0020, Tag.AffectedSOPClassUID, 0, Tag.MessageID,
            Status.SOPclassNotSupported, C_FIND_RSP),
    C_MOVE_RSP (0x8021, Tag.AffectedSOPClassUID, 0, Tag.MessageIDBeingRespondedTo,
            0, null),
    C_MOVE_RQ (0x0021, Tag.AffectedSOPClassUID, 0, Tag.MessageID,
            Status.SOPclassNotSupported, C_MOVE_RSP),
    C_ECHO_RSP (0x8030, Tag.AffectedSOPClassUID, 0, Tag.MessageIDBeingRespondedTo,
            0, null),
    C_ECHO_RQ (0x0030, Tag.AffectedSOPClassUID, 0, Tag.MessageID,
            Status.SOPclassNotSupported, C_ECHO_RSP),
    N_EVENT_REPORT_RSP (0x8100, Tag.AffectedSOPClassUID, Tag.AffectedSOPInstanceUID, Tag.MessageIDBeingRespondedTo,
            0, null),
    N_EVENT_REPORT_RQ (0x0100, Tag.AffectedSOPClassUID, Tag.AffectedSOPInstanceUID, Tag.MessageID,
            Status.NoSuchSOPclass, N_EVENT_REPORT_RSP),
    N_GET_RSP (0x8110, Tag.AffectedSOPClassUID, Tag.AffectedSOPInstanceUID, Tag.MessageIDBeingRespondedTo,
            0, null),
    N_GET_RQ (0x0110, Tag.RequestedSOPClassUID, Tag.RequestedSOPInstanceUID, Tag.MessageID,
            Status.NoSuchSOPclass, N_GET_RSP),
    N_SET_RSP (0x8120, Tag.AffectedSOPClassUID, Tag.AffectedSOPInstanceUID, Tag.MessageIDBeingRespondedTo,
            0, null),
    N_SET_RQ (0x0120, Tag.RequestedSOPClassUID, Tag.RequestedSOPInstanceUID, Tag.MessageID,
            Status.NoSuchSOPclass, N_SET_RSP),
    N_ACTION_RSP (0x8130, Tag.AffectedSOPClassUID, Tag.AffectedSOPInstanceUID, Tag.MessageIDBeingRespondedTo,
            0, null),
    N_ACTION_RQ (0x0130, Tag.RequestedSOPClassUID, Tag.RequestedSOPInstanceUID, Tag.MessageID,
            Status.NoSuchSOPclass, N_ACTION_RSP),
    N_CREATE_RSP (0x8140, Tag.AffectedSOPClassUID, Tag.AffectedSOPInstanceUID, Tag.MessageIDBeingRespondedTo,
            0, null),
    N_CREATE_RQ (0x0140, Tag.AffectedSOPClassUID, Tag.AffectedSOPInstanceUID, Tag.MessageID,
            Status.NoSuchSOPclass, N_CREATE_RSP),
    N_DELETE_RSP (0x8150, Tag.AffectedSOPClassUID, Tag.AffectedSOPInstanceUID, Tag.MessageIDBeingRespondedTo,
            0, null),
    N_DELETE_RQ (0x0150, Tag.RequestedSOPClassUID, Tag.RequestedSOPInstanceUID, Tag.MessageID,
            Status.NoSuchSOPclass, N_CREATE_RSP),
    C_CANCEL_RQ (0x0FFF, 0, 0, Tag.MessageIDBeingRespondedTo,
            0, null);

    public final static int WITH_DATASET = 0;
    public final static int NO_DATASET = 0x0101;
    public final int commandField;
    public final int tagOfSOPClassUID;
    public final int tagOfSOPInstanceUID;
    public final int tagOfMessageID;
    public final int noSuchSOPClass;
    public final Dimse rsp;

    Dimse(int commandField, int tagOfSOPClassUID, int tagOfSOPInstanceUID, int tagOfMessageID, int noSuchSOPClass, Dimse rsp) {
        this.commandField = commandField;
        this.tagOfSOPClassUID = tagOfSOPClassUID;
        this.tagOfSOPInstanceUID = tagOfSOPInstanceUID;
        this.tagOfMessageID = tagOfMessageID;
        this.noSuchSOPClass = noSuchSOPClass;
        this.rsp = rsp;
    }

    DicomObject mkRQ(int msgID, String sopClassUID, String sopInstanceUID, int dataSetType) {
        DicomObject commandSet = new DicomObject();
        commandSet.setString(tagOfSOPClassUID, VR.UI, sopClassUID);
        commandSet.setInt(Tag.CommandField, VR.US, commandField);
        commandSet.setInt(Tag.MessageID, VR.US, msgID);
        commandSet.setInt(Tag.CommandDataSetType, VR.US, dataSetType);
        if (tagOfSOPInstanceUID != 0)
            commandSet.setString(tagOfSOPInstanceUID, VR.UI, sopInstanceUID);
        return commandSet;
    }

    public DicomObject mkRSP(DicomObject commandSet) {
        return mkRSP(commandSet, NO_DATASET, Status.Success);
    }

    public DicomObject mkRSP(DicomObject rq, int dataSetType, int status) {
        DicomObject commandSet = new DicomObject();
        commandSet.setString(Tag.AffectedSOPClassUID, VR.UI, rq.getStringOrElseThrow(tagOfSOPClassUID));
        commandSet.setInt(Tag.CommandField, VR.US, rsp.commandField);
        commandSet.setInt(Tag.MessageIDBeingRespondedTo, VR.US, rq.getIntOrElseThrow(Tag.MessageID));
        commandSet.setInt(Tag.CommandDataSetType, VR.US, dataSetType);
        commandSet.setInt(Tag.Status, VR.US, status);
        if (tagOfSOPInstanceUID != 0)
            commandSet.setString(Tag.AffectedSOPInstanceUID, VR.UI, rq.getStringOrElseThrow(tagOfSOPInstanceUID));
        return commandSet;
    }

    public Object toString(Byte pcid, DicomObject commandSet, String tsuid) {
        return promptTo(pcid, commandSet, tsuid, new StringBuilder(256)).toString();
    }

    private StringBuilder promptTo(Byte pcid, DicomObject commandSet, String tsuid, StringBuilder sb) {
        promptHeaderTo(commandSet, sb);
        sb.append("[pcid: ").append(pcid & 0xff);
        commandSet.getInt(Tag.Status)
                .ifPresent(status -> sb
                        .append(", status: ")
                        .append(Integer.toHexString(status))
                        .append('H'));
        commandSet.getString(tagOfSOPClassUID)
                .ifPresent(uid -> UIDUtils.promptTo(uid, sb
                        .append(System.lineSeparator())
                        .append("  sop-class: ")));
        if (tagOfSOPInstanceUID != 0) {
            commandSet.getString(tagOfSOPInstanceUID)
                    .ifPresent(uid -> UIDUtils.promptTo(uid, sb
                            .append(System.lineSeparator())
                            .append("  sop-instance: ")));
        }
        UIDUtils.promptTo(tsuid, sb
                        .append(System.lineSeparator())
                        .append("  transfer-syntax: "));
        return sb.append(']');
    }

    private StringBuilder promptHeaderTo(DicomObject commandSet, StringBuilder sb) {
        return sb.append(commandSet.getIntOrElseThrow(tagOfMessageID))
                .append(':')
                .append(name().replace('_', '-'));
    }

    static boolean hasDataSet(DicomObject commandSet) {
        return commandSet.getIntOrElseThrow(Tag.CommandDataSetType) != NO_DATASET;
    }
}
