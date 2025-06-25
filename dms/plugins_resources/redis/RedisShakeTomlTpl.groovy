package redis

def type = super.binding.getProperty('type') as String

def srcType = super.binding.getProperty('srcType') as String
def srcAddress = super.binding.getProperty('srcAddress') as String
def srcUsername = super.binding.getProperty('srcUsername') as String
def srcPassword = super.binding.getProperty('srcPassword') as String

def targetType = super.binding.getProperty('targetType') as String
def targetAddress = super.binding.getProperty('targetAddress') as String
def targetUsername = super.binding.getProperty('targetUsername') as String
def targetPassword = super.binding.getProperty('targetPassword') as String

"""
[${type}_reader]
cluster = "${srcType == 'cluster' ? 'true' : 'false'}"
address = "${srcAddress}"
username = "${srcUsername}"
password = "${srcPassword}"
[redis_writer]
cluster = "${targetType == 'cluster' ? 'true' : 'false'}"
address = "${targetAddress}" 
username = "${targetUsername}"
password = "${targetPassword}"
"""