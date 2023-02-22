<!--
==============================================================================
 This software is part of the Open Standard for Unattended Sensors (OSUS)
 reference implementation (OSUS-R).

 To the extent possible under law, the author(s) have dedicated all copyright
 and related and neighboring rights to this software to the public domain
 worldwide. This software is distributed without any warranty.

 You should have received a copy of the CC0 Public Domain Dedication along
 with this software. If not, see
 <http://creativecommons.org/publicdomain/zero/1.0/>.
==============================================================================
-->

# Open Standard for Unattended Sensors (OSUS)
Download release artifacts for the reference implementation from Github [releases](https://github.com/ssg-udri/OSUS-R/releases).
To build OSUS-R, follow these [instructions](BUILD-README.md).

## Introduction
OSUS, as described in the OSUS Standard, provides a standardized set of services that allows multiple vendors to produce
OSGi compliant bundles written in Java that execute on any OSUS platform, as long as the specific platform has the
required (if any) hardware interfaces and platform interface implementation. The platform interface gives plug-ins the
ability to access hardware through standardized software interfaces. The OSUS Core abstracts all platform specific
dependencies away from bundles. OSUS plug-ins are types of OSGi bundles, and the term plug-in is used to refer to a
specific type of OSUS bundle, which can be an asset, a communication layer, or an extension that interacts with the
OSUS Application Programming Interface (API).

## OSGi Framework
The OSUS environment uses the OSGi Framework (see OSGi Core Specification) to support each plug-in. The OSGi framework
is an open standard, extensible framework. The framework allows functionality to be added to a controller in the form of
bundles without redeploying a system in its entirety. In addition, existing bundles can be updated or removed. This is
important for upgrades in the field and for inexpensive product modifications/additions.

OSGi has a service layer, using a publish, find and bind model which promotes modular, maintainable code. A service is a
normal Java object that is registered with the service registry under one or more Java interfaces. Bundles can register
services, search for services, or receive notifications when their registration state changes. OSUS provides standard
OSGi services and the OSGi community provides several services as well.

In addition, the OSGi Declarative Services specification is included in the OSUS Standard to make it very easy to publish
services and bind to services. This minimizes the code a plug-in developer has to write and allows components to only be
loaded when required. From a system perspective, this means reduced start-up time and a potentially smaller memory
footprint. See Section 112 of the OSGi Service Compendium for the complete specification.

For more detailed information about bundles and services, see the Bundles and Services section of the OSUS Standard.

# Getting Started

## Installation
Download a controller-app and the OSUS-R Operator Instructions document from [releases](https://github.com/ssg-udri/OSUS-R/releases).
Follow the instructions to install and run an OSUS-R controller.

## Create a Plug-in
Download sdk-app-generic.zip and the OSUS Plug-in Guide document from [releases](https://github.com/ssg-udri/OSUS-R/releases).
Follow the guide to create an OSUS plug-in and deploy on an OSUS-R controller.

## Versions
1.x Used by international customers in the Technical Cooperation Program's (TTCP) Contested Urban Environment Strategic (CUE).
2.x This is not backward compatiable with version 1.x
