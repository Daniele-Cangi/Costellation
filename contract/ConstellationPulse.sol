// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ConstellationPulse {
    mapping(uint40 => mapping(address => bool)) public sealed;

    uint8 public constant MAX_SIGNAL = 100;
    uint16 public constant MAX_MESSAGE_BYTES = 280;

    event PulseStamped(
        address indexed user,
        uint40 indexed yyyymmdd,
        string message,
        uint8 valence,
        uint8 arousal,
        uint8 energy,
        uint8 focus,
        uint8 social
    );

    function seal(
        uint40 yyyymmdd,
        string calldata message,
        uint8 valence,
        uint8 arousal,
        uint8 energy,
        uint8 focus,
        uint8 social
    ) external {
        require(!sealed[yyyymmdd][msg.sender], "already sealed");
        require(_validDate(yyyymmdd), "invalid date");
        require(bytes(message).length <= MAX_MESSAGE_BYTES, "message too long");
        require(_validSignal(valence), "invalid valence");
        require(_validSignal(arousal), "invalid arousal");
        require(_validSignal(energy), "invalid energy");
        require(_validSignal(focus), "invalid focus");
        require(_validSignal(social), "invalid social");

        sealed[yyyymmdd][msg.sender] = true;
        emit PulseStamped(msg.sender, yyyymmdd, message, valence, arousal, energy, focus, social);
    }

    function _validSignal(uint8 value) private pure returns (bool) {
        return value <= MAX_SIGNAL;
    }

    function _validDate(uint40 yyyymmdd) private pure returns (bool) {
        uint40 year = yyyymmdd / 10000;
        uint40 month = (yyyymmdd / 100) % 100;
        uint40 day = yyyymmdd % 100;

        return year >= 2025 && month >= 1 && month <= 12 && day >= 1 && day <= 31;
    }
}
