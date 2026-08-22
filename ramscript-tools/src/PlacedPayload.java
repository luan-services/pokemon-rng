record PlacedPayload(PayloadStorageArea area, byte[] payload) {
    PlacedPayload {
        if (area == null) throw new IllegalArgumentException("area must not be null");
        if (payload == null || payload.length == 0) throw new IllegalArgumentException("payload must not be empty");
        if (payload.length > area.capacity()) {
            throw new IllegalArgumentException("payload does not fit in " + area + ": " + payload.length + " > " + area.capacity());
        }
        payload = payload.clone();
    }

    @Override
    public byte[] payload() { return payload.clone(); }
}
